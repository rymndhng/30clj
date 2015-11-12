(ns cheerleader.app
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [cheerleader.fmt :as fmt]))

(defn readable-time [date-obj]
  "Formats to HH:MM:SS"
  (-> date-obj .toTimeString (clojure.string/split " ") first))

(defonce time-updater (js/setInterval #(rf/dispatch [:tick-timer (js/Date.)]) 1000))

(def init-state {:time (js/Date.)

                 :timer {:started?   nil
                         :start-time nil
                         :interval   nil
                         :break      nil
                         :count      0
                         :count-max  nil}

                 ;; When the form value changes, it's binded to this data structure
                 :bind-form {:interval nil
                             :break    nil
                             :count    nil}

                 ;; List of Schedules to Show and Run
                 :schedules [{:name "Dog Pose"
                              :interval 30
                              :break    15
                              :reps     5}]})

;; -- Event Handlers -----------------------------------------------------------
(rf/register-handler
  :initialize
  (fn
    [db _]
    (js/console.log "Hello")
    (merge db init-state)))

(rf/register-handler
  :tick-timer
  (fn [db [_ value]]
    (assoc db :time value)))

(rf/register-handler
  :trigger-start-stop
  (fn [db [_ value]]
    (update db :timer
      (fn [timer]
        (-> timer
          (update :started? #(not %))
          (assoc :start-time (js/Date.))
          (assoc :interval (js/parseInt (get-in db [:bind-form :interval])))
          (assoc :break    (js/parseInt (get-in db [:bind-form :break])))
          )))))

(rf/register-handler
  :update-form
  (fn [db [_ field value]]
    (assoc-in db [:bind-form field] value)))

;; -- Subscription Handlers ----------------------------------------------------
(rf/register-sub
  :time
  (fn
    [db _]                              ;; db is the app-db atom
    (reaction (:time @db))))          ;; wrap the computation in a reaction

(rf/register-sub
  :timer-state
  (fn
    [db _]
    (reaction (:timer @db))))

(rf/register-sub
  :current-timer
  (fn
    [db _]
    (reaction (let [current-time (:time @db)
                    {:keys [interval break start-time started?]} (:timer @db)]
                (if started?
                  (let [full-period (+ break interval)
                        animate-seconds (-> full-period
                                          Math/round
                                          (str "s"))
                        elapsed (-> (- (.getTime current-time)
                                       (.getTime start-time))
                                  (/ 1000)
                                  Math/round
                                  (mod full-period))]
                    {:full-period full-period
                     :animate-seconds animate-seconds
                     :elapsed elapsed})
                  {})))))

(rf/register-sub
  :form-state
  (fn [db _]
    (reaction (:bind-form @db))))

;; -- view components ----------------------------------------------------------
(defn read-input
  "Reads input tab succinctly"
  [element]
  (-> element .-target .-value))

;; TODO: make a macro for form controls instead
(defn form-group [label & body]
  [:div.form-group {:key label}
   [:label.control-label label]
   [:div
    body]])

(defn input-group
  [form-value bind-field handler]
  [form-group
   (fmt/readable-key bind-field)
   ^{:key bind-field}
   [:input.form-control {:type "text"
                         :on-change #(rf/dispatch [handler bind-field (read-input %)])
                         :value (bind-field form-value)}]])

(defn form-control
  "Controls the form input."
  []
  (let [form-state (rf/subscribe [:form-state])
        timer-state (rf/subscribe [:timer-state])]
    (fn form-render []
      (let [form-value @form-state
            timer-value @timer-state]
        [:form.form-inline
         (when (not (:started? timer-value))
           [:div
            [input-group form-value :interval :update-form]
            [input-group form-value :break    :update-form]])
         (form-group ""
           [:input.btn.btn-primary {:key :submit
                                    :type "button"
                                    :value "Start/Stop"
                                    :on-click #(rf/dispatch [:trigger-start-stop])}])]))))

(defn greeting [message]
  [:h1 message])

(defn slider []
  (let [timer (rf/subscribe [:timer-state])
        current-timer (rf/subscribe [:current-timer])]
    (fn []
      (let [{:keys [interval break started?]} @timer
            {:keys [full-period animate-seconds elapsed]} @current-timer]
        (when started?
          (do
            (print (str elapsed ":" full-period))
            [:div.scanline
             [:svg {:width "100%" :height "100%"
                    :style {:shape-rendering "auto"}}
              [:rect {:width (fmt/ratio-to-css-pct (/ interval full-period))
                      :height "100%"
                      :style {:fill "#99D2E4"}}]
              [:rect {:x (fmt/ratio-to-css-pct (/ interval full-period))
                      :width (fmt/ratio-to-css-pct (/ break full-period))
                      :height "100%"
                      :style {:fill "#FFD4DA"}}]
              [:rect {:width (fmt/ratio-to-css-pct (/ elapsed full-period))
                      :height "100%"
                      :style {:fill "#FFF"
                              :opacity 0.7}}]]]))))))

(defn clock
  []
  (let [timer (rf/subscribe [:time])]
    (fn clock-render
      []
      (let [time-str (readable-time @timer)]
        [:div.example-clock time-str]))))

(defn simple-example
  []
  [:div
   [greeting "Hello world, it is now"]
   [clock]
   [slider]
   [form-control]])

(defn clock-component
  []
  (let [time (rf/subscribe [:current-time])]
    (fn []
      [:div "Current Time" (readable-time time)])))

;; -- Entry Point --------------------------------------------------------------
(defn init
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [simple-example]
    (js/document.getElementById "container")))

;; NOTE: see reagent/db for the actual database state ;)
(comment
  (require '[re-frame.db :as rdb])
  @rdb/app-db)
