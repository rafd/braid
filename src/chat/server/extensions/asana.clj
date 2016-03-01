(ns chat.server.extensions.asana
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [org.httpkit.client :as http :refer [url-encode]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [chat.server.db :as db]
            [chat.server.cache :refer [cache-set! cache-get cache-del! random-nonce]]
            [chat.server.extensions :refer [redirect-uri webhook-uri
                                            handle-thread-change handle-webhook handle-oauth-token
                                            extension-config str->b64 b64->str edn-response]]))

(def client-id (env :asana-client-id))
(def client-secret (env :asana-client-secret))

;; Setup

(defn create-asana-extension
  [{:keys [id group-id tag-id]}]
  (db/with-conn
    (db/create-extension! {:id id
                           :group-id group-id
                           :config {:type :asana
                                    :tag-id tag-id}})))


;; Authentication flow
(def token-url "https://app.asana.com/-/oauth_token")
(def authorization-url "https://app.asana.com/-/oauth_authorize")
(def api-url "https://app.asana.com/api/1.0")

(defn auth-url
  [extension-id]
  (let [nonce (random-nonce 20)
        info (-> {:extension-id extension-id
                  :nonce nonce}
                 pr-str str->b64)]
    (cache-set! (str extension-id) nonce)
    (str authorization-url
         "?client_id=" (url-encode client-id)
         "&redirect_uri=" (url-encode redirect-uri)
         "&response_type=" "code"
         "&state=" (url-encode info))))

; TODO: can probably factory pretty much all of this except the URL out
(defmethod handle-oauth-token :asana
  [_ state code]
  (let [{ext-id :extension-id sent-nonce :nonce} (-> state b64->str edn/read-string)]
    (when-let [stored-nonce (cache-get (str ext-id))]
      (cache-del! (str ext-id))
      (when (= stored-nonce sent-nonce)
        (let [resp @(http/post token-url
                      {:form-params {"grant_type" "authorization_code"
                                     "client_id" client-id
                                     "client_secret" client-secret
                                     "redirect_uri" redirect-uri
                                     "code" code}})]
          (if (= 200 (:status resp))
            (let [{:strs [access_token refresh_token]} (json/read-str (:body resp))]
              (db/with-conn
                (db/save-extension-token! ext-id {:access-token access_token
                                                  :refresh-token refresh_token})))
            (timbre/warnf "Bad response when exchanging token %s" (:body resp))))))))

(defn refresh-token
  [ext]
  (let [refresh-tok (:refresh-token ext)]
    (let [resp @(http/post token-url
                           {:form-params {"grant_type" "refresh_token"
                                          "client_id" client-id
                                          "client_secret" client-secret
                                          "redirect_uri" redirect-uri
                                          "refresh_token" refresh-tok}})]
      (if (= 200 (:status resp))
        (let [{:strs [access_token refresh_token]} (json/read-str (:body resp))]
          (db/with-conn
            (db/save-extension-token! (ext :id) {:access-token access_token
                                                 :refresh-token (or refresh_token
                                                                    refresh-tok)})))
        (do (timbre/warnf "Bad response when exchanging token %s" (:body resp))
            nil)))))

;; Webhooks
(defn register-webhook
  [extension-id resource-id]
  (let [ext (db/with-conn (db/extension-by-id extension-id))]
    (http/post (str api-url "/webhooks")
               {:oauth-token (:token ext)
                :form-params {"resource" resource-id
                              "target" (str webhook-uri "/" (:id ext))}})))

(defmethod handle-webhook :asana
  [extension event-req]
  (if-let [secret (get-in event-req [:headers "X-Hook-Secret"])]
    {:status 200 :headers {"X-Hook-Secret" secret}}
    (do (println "webhook" event-req)
        {:status 200})))

;; watched thread notification

(defmethod handle-thread-change :asana
  [extension thread-id]
  (println "ext" extension "Thread changed" thread-id))

;; Fetching information
(defn fetch-asana-info
  [ext-id path]
  (let [ext (db/with-conn (db/extension-by-id ext-id))
        resp @(http/get (str api-url path)
                        {:oauth-token (ext :token)})]
      (if (= 200 (:status resp))
        (-> resp :body json/read-str)
        (do (timbre/warnf "token expired %s" (:body resp))
            (if (refresh-token ext)
              (fetch-asana-info ext-id path)
              (timbre/warnf "Failed to refresh token"))))))

(defn available-workspaces
  [ext-id]
  (-> (fetch-asana-info ext-id "/users/me")
      (get-in ["data" "workspaces"])))

(defn workspace-projects
  [ext-id workspace-id]
  (-> (fetch-asana-info ext-id (str "/workspaces/" workspace-id "/projects"))
      (get "data")))

;; configuring
(defn projects
  [ext]
  (if (ext :token)
    (edn-response
      {:ok true
       :workspaces
       (doall
         (map (fn [{:strs [id name]}]
                {:id id
                 :name name
                 :projects (workspace-projects (ext :id) id)})
              (available-workspaces (ext :id))))})
    (edn-response {:error "no asana token"} 400)))

(defn select-project
  [ext project-id]
  (db/with-conn
    (db/set-extension-config! (ext :id) :project-id project-id))
  (register-webhook (ext :id) project-id)
  (edn-response {:ok true}))

(defmethod extension-config :asana
  [ext [method args]]
  (case method
    :select-project (select-project ext (first args))
    :get-projects (projects ext)))

