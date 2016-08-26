(ns braid.client.calls.views
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.ui.views.pills :refer [user-pill-view]]
            [braid.client.webrtc :as rtc]
            [re-frame.core :refer [dispatch subscribe]]))

(defn ended-call-view
  [call]
  (let [correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch [:calls/set-requester-call-status [call :archived]]))} "X"]
        [:p (str "Call with " @correct-nickname " ended")]])))

(defn dropped-call-view
  [call]
  (let [correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch [:calls/set-requester-call-status [call :archived]]))} "X"]
        [:p (str "Call with " @correct-nickname " dropped")]])))

(defn declined-call-view
  [call]
  (let [user-is-caller? (subscribe [:current-user-is-caller? (call :caller-id)])
        caller-nickname (subscribe [:nickname (call :caller-id)])
        callee-nickname (subscribe [:nickname (call :callee-id)])]
    (fn [call]
      [:div
        [:a.button {:on-click (fn [_] (dispatch [:calls/set-requester-call-status [call :archived]]))} "X"]
        (if @user-is-caller?
          [:p (str @callee-nickname " declined your call")]
          [:p (str "Call with " @caller-nickname "declined")])])))

(defn accepted-call-view
  [call]
  (let [call-time (r/atom 0)
        correct-nickname (subscribe [:correct-nickname call])]
    (fn [call]
      (js/setTimeout #(swap! call-time inc) 1000)
      [:div
       [:h4 (str "Call with " @correct-nickname "...")]
       [:div (str @call-time)]
       [:br]
       [:a.button "A"]
       [:a.button "M"]
       [:a.button "V"]
       [:video {:id "vid"
                :class (if (= (call :type) :video) "video" "audio")}]
       [:a.button {:on-click
                    (fn [_]
                      (dispatch [:calls/set-requester-call-status [call :ended]]))} "End"]])))

(defn incoming-call-view
  [call]
  (let [user-is-caller? (subscribe [:current-user-is-caller? (call :caller-id)])
        caller (subscribe [:user (call :caller-id)])
        callee (subscribe [:user (call :callee-id)])]
    (fn [call]
      (if @user-is-caller?
        [:div
           [:p (str "Calling " (@callee :nickname)"...")]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch [:calls/set-requester-call-status [call :dropped]]))} "Drop"]]
        [:div
           [:p (str "Call from " (@caller :nickname))]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch [:calls/set-requester-call-status [call :accepted]]))} "Accept"]
           [:a.button {:on-click
                        (fn [_]
                          (dispatch [:calls/set-requester-call-status [call :declined]]))} "Decline"]]))))

(defn during-call-view
  [call]
  [:div.call
   (case (call :status)
     :incoming [incoming-call-view call]
     :accepted [accepted-call-view call]
     :declined [declined-call-view call]
     :dropped [dropped-call-view call]
     :ended [ended-call-view call])])

(defn call-view []
  (let [new-call (subscribe [:new-call])]
    (fn []
      (when @new-call
        [during-call-view @new-call]))))
