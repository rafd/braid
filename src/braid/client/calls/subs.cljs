(ns braid.client.calls.subs
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [re-frame.core :refer [subscribe reg-sub-raw]]
            [braid.client.state.subscription :refer [subscription]]))

(reg-sub-raw
  :calls
  (fn [state _]
    (reaction (vals (@state :calls)))))

(reg-sub-raw
  :call-status
  (fn [state [_ call]]
    (reaction (get-in @state [:calls (call :id) :status]))))

(reg-sub-raw
  :new-call
  (fn [state _]
    (reaction (->> (@state :calls)
                   vals
                   (filter (fn [c] (not= :archived (c :status))))
                   (sort-by :created-at)
                   first))))

(reg-sub-raw
  :current-user-is-caller?
  (fn [state [_ caller-id]]
    (reaction (= (get-in @state [:session :user-id]) caller-id))))

(reg-sub-raw
  :correct-nickname
  (fn [state [_ call]]
    (let [is-caller? (reaction @(subscribe [:current-user-is-caller? (call :caller-id)]))
          caller-nickname (reaction @(subscribe [:nickname (call :caller-id)]))
          callee-nickname (reaction @(subscribe [:nickname (call :callee-id)]))]
      (reaction (if @is-caller? @callee-nickname @caller-nickname)))))

(reg-sub-raw
  :user-status
  (fn [state [_ user-id]]
    (reaction (get-in @state [:users user-id :status]))))
