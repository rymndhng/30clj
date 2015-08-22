(ns server
  (:gen-class)
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.params :as params]
            [compojure.core :as c]
            [compojure.route :as r]
            [hiccup.core :as h]))

(defonce counter (atom 0))

(def template
  "Takes a single argument: module -- the clojurescript module to load."
  (h/html
    [:html
     [:head
      [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"}]
      [:script {:typech "text/javascript" :src "public/js/main.js"}]]
     [:body
      [:div {:class "container"}
       [:div {:class "row"}
        [:div {:class "col-xs-12"}
         [:h1 "A Demo Below"]]]
       [:div {:class "row"}
        [:div {:class "col-xs-12"}
         [:div.well.text-center
          [:h1#value "- Value -"]]
         [:button#btn.btn.btn-primary "CLICK ME to INC"]]]]]]))


(defn web-handler []
  {:status 200
   :headers {"content-type" "text/html"}
   :body template})

(defn ws-handler
  [req]
  (d/let-flow [conn (d/catch (http/websocket-connection req)
                        #(println @%))]
    (if-not conn
      {:status 400
       :headers {"content-type" "application/text"}
       :body "Expected a websocket request."}

      (let [session-id (gensym "watcher")
            <out (s/stream 1 (map pr-str))]
        (add-watch counter session-id (fn [_ _ old new]
                                        (println "Watched change: " old " " new)
                                        (s/put! <out @counter)
                                        ))
        (s/consume (fn [_]
                     (println "CLICKED!!")
                     (swap! counter inc))
          conn)
        (s/connect <out conn)
        (s/on-closed conn #(do (println "shutdown!")
                               (remove-watch counter session-id)))
        (println "Nearly done boostrapping")
        ;; put initial value in
        (s/put! <out @counter))))
  ;; TODO: this is kind of meaningless to return somethin
  "Done!")

(def handler
  (params/wrap-params
    (c/routes
      (c/GET  "/ws" [] ws-handler)
      (c/GET  "/" [] (web-handler))
      (r/resources "/public")
      (r/not-found "No such page."))))

(defn -main
  [& args]
  (let [port (or (first args) 2000)
        server-var (find-var 'server/server)]
    (when (bound? server-var)
        (.close (var-get server-var)))
    (println "Serving on port " port)
    (def server (http/start-server handler {:port port}))))
