(ns braid.search.lucene
  (:require
   [braid.base.conf :refer [config]]
   [clucie.analysis :as analysis]
   [clucie.core :as clucie]
   [clucie.store :as store]
   [clucie.query :as query]
   [clucie.utils]
   [clojure.string :as string]
   [datomic.api :as d]
   [braid.core.server.db :as db]
   [braid.chat.db.thread :as db.thread]
   [braid.chat.db.common :as common])
  (:import
   (org.apache.lucene.analysis.core LowerCaseFilter)
   (org.apache.lucene.analysis.icu ICUNormalizer2CharFilterFactory)
   (org.apache.lucene.analysis.standard StandardTokenizer)
   (org.apache.lucene.index IndexNotFoundException)
   (org.apache.lucene.search BooleanClause$Occur)
   (org.apache.lucene.util QueryBuilder)))

(extend-protocol query/FormParsable
  java.util.UUID
  (parse-formt [u {:keys [^QueryBuilder builder key]}]
    ;; UUIDs should always match exactly
    (.createBooleanQuery builder (clucie.utils/stringify-value key) (str u)
                         BooleanClause$Occur/MUST)))

(def analyzer
  (analysis/build-analyzer
    (StandardTokenizer.)
    :char-filter-factories [(ICUNormalizer2CharFilterFactory.
                              (java.util.HashMap. {"name" "nfkc"
                                                   "mode" "compose"}))]
    :token-filters [(LowerCaseFilter.)]))

(defonce -store (atom nil))
(defn store
  []
  (swap! -store
         (fn [s]
           (or s
               (if-let [store-path (config :lucene-store-location)]
                 (store/disk-store store-path)
                 (store/memory-store))))))

(defonce writers-cache (agent {}))
(defn writer
  [store]
  ;; [TODO] use a promise instead of awaiting?
  (send writers-cache update store
        (fn [writer]
          (or writer (store/store-writer store analyzer))))
  (await-for 1000 writers-cache)
  (get @writers-cache store))

(defn index-message!
  ([message] (index-message! (store) message))
  ([reader message]
   (let [message (-> message
                     (update :created-at (memfn getTime))
                     (assoc :group-id (db.thread/thread-group-id (:thread-id message))))
         wrtr (writer (store))]
     (if-let [existing (try
                         (first
                           (clucie/search reader {:group-id (:group-id message)
                                                 :thread-id (:thread-id message)}
                                          1))
                         (catch IndexNotFoundException _
                           nil))]
       (clucie/update!
         wrtr
         (update existing :content str "\n" (:content message))
         [:group-id :thread-id :content]
         :thread-id (:thread-id message))
       (clucie/add!
         wrtr
         [message]
         [:group-id :thread-id :content]))
     (.commit wrtr))))

(defn search
  ([group-id text] (search (store) group-id text))
  ([reader group-id text]
   (->> (clucie/search
         reader
         {:group-id group-id
          :content (string/split text #"\s+")}
         1000
         analyzer
         0
         100)
       (map (fn [{:keys [thread-id created-at] :as res}]
              {:thread-id (java.util.UUID/fromString thread-id)
               :created-at (Long. created-at)}))
       (group-by :thread-id)
       (into #{}
             (map (fn [[thread-id threads]]
                    [thread-id (java.util.Date. (apply max (map :created-at threads)))]))))))

(defn import-messages!
  "Import all existing messages into Lucene"
  []
  ;; Need to have an index created to make a reader, so just insert a dummy record
  (index-message!
    (store)
    {:content "hello" :group-id (java.util.UUID/randomUUID)
     :thread-id (java.util.UUID/randomUUID) :created-at (java.util.Date.)})
  (with-open [reader (store/store-reader (store))]
    (doseq [msg (->> (d/q '[:find [(pull ?m pull-pattern) ...]
                           :in $ pull-pattern
                           :where [?m :message/id _]]
                         (db/db)
                         common/message-pull-pattern)
                    (map common/db->message))]
      (when (and (:group-id msg) (:thread-id msg) (:content msg)
                 (:created-at msg))
        (index-message! reader msg)))))
