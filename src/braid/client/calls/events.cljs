(ns braid.client.calls.events
  (:require [braid.client.webrtc :as rtc]
            [re-frame.core :refer [dispatch reg-event-db]]
            [braid.client.sync :as sync]
            [braid.client.schema :as schema]
            [braid.client.calls.helpers :as helpers]))

(reg-event-db
  :calls/start-new-call
  (fn [state [_ data]]
    (rtc/get-ice-servers
      (fn [servers]
        (rtc/create-connections servers)
        (let [call (schema/make-call data)]
          (sync/chsk-send! [:braid.server/make-new-call call])
          (dispatch [:calls/add-new-call call]))))
    state))

(reg-event-db
  :calls/receive-new-call
  (fn [state [_ call]]
    (rtc/get-ice-servers
      (fn [servers]
        (rtc/create-connections servers)
        (dispatch [:calls/add-new-call call])))
    state))

(reg-event-db
  :calls/add-new-call
  (fn [state [_ call]]
    (helpers/add-call state call)))

(reg-event-db
  :calls/set-requester-call-status
  (fn [state [_ [call status]]]
    (when (= status :accepted)
      (rtc/set-stream))
    (sync/chsk-send! [:braid.server/change-call-status {:call call :status status}])
    (helpers/set-call-status state (call :id) status)))

(reg-event-db
  :calls/set-receiver-call-status
  (fn [state [_ [call status]]]
    (when (= status :accepted)
      (rtc/set-stream))
    (helpers/set-call-status state (call :id) status)))
