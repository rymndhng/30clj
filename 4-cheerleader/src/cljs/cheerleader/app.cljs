(ns cheerleader.app
  (:require [reagent.core :as reagent :refer [atom]]))

(defonce timer       (reagent/atom (js/Date.)))
(defonce time-color  (reagent/atom "#000"))
(defonce start-pulse (reagent/atom false))
(defonce in-pulse    (reagent/atom false))

;; This is finnicky because CSS animations are hard to control on reload
(defonce timer-state (reagent/atom {:started?       nil
                                    :start-time     nil
                                    :interval       nil
                                    :break          nil
                                    :count          0}))

(def default-voice (->> (.getVoices js/window.speechSynthesis)
                     (filter #(= "en-GB" (.-lang %)))
                     first))

(def speak-start-rep
  (doto (js/SpeechSynthesisUtterance. "Go!")
    (aset "voice" default-voice)))

(def speak-take-break
  (doto (js/SpeechSynthesisUtterance. "Stap!")
    (aset "voice" default-voice)))

(defn speak [msg]
  (.speak js/window.speechSynthesis msg))

(defonce animation-listener
  (doall
    (map #(.addEventListener
            (.getElementById js/document "container")
            (first %) (second %))
      ;; Hacks but this essentially only happens when the progressbar ends
      [["animationiteration" (fn []
                               )]
       ["animationend" (fn []
                         (js/console.log "IT ENDED!")
                         (swap! timer-state (fn [x]
                                              (-> x
                                                (update :count inc)
                                                (assoc :start-time (js/Date.)))))
                         (speak speak-start-rep)

                         ;; Need to specialize on this.
                         (reset! start-pulse true)
                         (reset! in-pulse false))]
       ])))

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))

(defn color-input []
  [:div.color-input
   [:input {:type "color"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defonce form-state (reagent/atom {:interval 0
                                   :break    0}))

(defn timer-control []
  [:div.col-xs-12
   [:form.form-horizontal
    [:div.form-group
     [:label.col-xs-3.col-xs-offset-2.control-label "Interval"]
     [:div.col-xs-5
      [:input.form-control.input-lg {:type "text"
                                     :placeholder "Interval"
                                     :on-change #(swap! form-state assoc :interval
                                                   (-> % .-target .-value js/parseInt (* 1000)))}]]]
    [:div.form-group
     [:label.col-xs-3.col-xs-offset-2.control-label "Rest Interval"]
     [:div.col-xs-5
      [:input.form-control.input-lg {:type "text"
                                     :placeholder "Rest Interval"
                                     :on-change #(swap! form-state assoc :break
                                                   (-> % .-target .-value js/parseInt (* 1000)))}]]]
    [:div.form-group
     [:div.col-xs-5.col-xs-offset-5
      [:button.btn.btn-primary.btn-block.btn-lg
       {:on-click (fn [e]
                    (.preventDefault e)
                    (reset! timer-state (merge @form-state
                                          {:started? true
                                           :start-time (js/Date.)
                                           :count 0})))}
       "Reset!"]]]]])


(defn ratio-to-css-pct [ratio]
  (-> ratio
    (* 100)
    str
    (subs 0 10)
    (str "%")))

(defn show-elapsed-time []
  (let [{:keys [interval break start-time started?]} @timer-state
        full-period (+ break interval)
        animate-seconds (-> (/ full-period 1000) Math/round
                              (str "s"))
        elapsed     (-> (- (.getTime @timer)
                          (.getTime start-time))
                      (mod full-period))]
    [:div.scanline
     [:svg {:width "100%" :height "100%"
            :style {:shape-rendering "auto"}}
      [:rect {:width (ratio-to-css-pct (/ interval full-period))
              :height "100%"
              :style {:fill "#99D2E4"}}]
      [:rect {:x (ratio-to-css-pct (/ interval full-period))
              :width (ratio-to-css-pct (/ break full-period))
              :height "100%"
              :style {:fill "#FFD4DA"}}]
      ;; FIXME: how do I only redraw this when start-time changes?
      ^{:key start-time}
      [:rect {:width "100%"
              :height "100%"
              :style {:fill "#FFF"
                      :opacity 0.7
                      :animation-name "slide"
                      :animation-duration animate-seconds
                      :animation-timing-function "linear"}}
       ]]]))

(defn stats-view []
  [:div (str "Count:" (:count @timer-state))])

(defn simple-example []
  (when @start-pulse
    (reset! start-pulse false)
    (reset! in-pulse true))
  [:div
   [:div.container-fluid (when @in-pulse {:className "pulser"})
    [:div.row
     [clock]
     (when (:started? @timer-state)
       [stats-view])]
    [:div.row
     [timer-control]]
    (when (:started? @timer-state)
      [show-elapsed-time])]])

(defn elapsed [start current period]
  (-> (- (.getTime current) (.getTime start))
    (mod period)))

;; Some global notifications
(defonce time-updater (js/setInterval #(reset! timer (js/Date.)) 1000))

(defonce notifiers
  (add-watch timer :notify
    (fn [key ref old new]
      (let [{:keys [start-time interval break]}  @timer-state]
        (when start-time
          (let [full-period (+ interval break)
                old-elapsed (elapsed start-time old full-period)
                new-elapsed (elapsed start-time new full-period)]
            (when (< old-elapsed interval new-elapsed)
              (speak speak-take-break)
              (js/console.log "Break Started"))))))))

(defn init []
  (reagent/render [simple-example]
    (js/document.getElementById "container")))
