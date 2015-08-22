(ns client
  (:require [cljs.reader :as reader]))

(enable-console-print!)

(.log js/console "hello world")

(defonce counter (atom 0))
(defonce websocket* (atom nil))

(defonce *ws-path*
  (let [base (-> (aget js/window "location" "href")
               (.split "?")
               first
               (.slice 5))]
    (str "ws:" base "ws")))

(add-watch counter :re-render
  (fn [_ _ _ _]
    (-> (.getElementById js/document "value")
      (aset "innerHTML" @counter))))

(defn main
  []
  (reset! websocket* (js/WebSocket. *ws-path*))
  (doall
    (map #(aset @websocket* (first %) (second %))
      [["onopen"    (fn []
                      (println "... websocket established!"))]
       ["onclose"   (fn []
                      (println "... websocket closed!"))]
       ["onerror"   (fn [e]
                      (println (str "WS-ERROR:" e)))]
       ["onmessage" (fn [m]
                      (println "Got: " (aget m "data"))
                      (reset! counter (reader/read-string (aget m "data"))))]]))



  (.addEventListener js/document "DOMContentLoaded" (fn []
      (-> (.getElementById js/document "btn")
        (aset "onclick" (fn [e]
                          (println "CLICKED!")
                          (.send @websocket* "update!"))))))

  (aset js/window "onunload"
    (fn [] (.close @websocket*)))
  (.log js/console "main!"))
