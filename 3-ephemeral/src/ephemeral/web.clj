(ns ephemeral.web
  (:require [ephemeral.db :as db]
            [compojure.core :as c :refer [GET POST]]
            [compojure.route :as route]
            [hiccup.core :as h]
            [hiccup.element :as he]
            [aleph.http :as http]
            [ring.middleware.stacktrace :as stacktrace]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.flash :as flash]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.util.response :as ring]))

(defn to-form-date
  "How to serialize into a date input."
  [^java.time.Instant date]
  (-> (java.time.format.DateTimeFormatter/ISO_LOCAL_DATE)
    (.withZone (java.time.ZoneId/systemDefault))
    (.format date)
    ))

(defn ^java.time.Instant from-form-date
  "If you give me a long one, I will shorten it"
  [text]
  (if-not text
    nil
    (-> (java.time.LocalDate/parse (subs text 0 10))
      (.atTime 0 0)  ;; midnight it
      (.atZone (java.time.ZoneId/systemDefault)) ;; at our timezone
      (.toInstant))))

(defn base-template
  [content]
  (h/html
    [:html
     [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:link {:rel "stylesheet"
              :href "/public/paper.bootstrap.min.css"}]
      [:body
       [:div.container
        [:div.row
         [:div.col-xs-12.col-sm-2.col-md-3]
         [:div.col-xs-12.col-sm-8.col-md-6
          [:h1 "Ephemeral Messages"]
          content
          [:a {:href "/"} "Send your own Ephemeral."]
          [:footer
           "Made by " [:a {:href "www.twitter.com/rymndhng"} "@rymndhng"]]]]]]]]))

(defn homepage
  [created?]
  (base-template
    (h/html
      [:h5 "Send an Ephemeral to a loved one!"]
      (when created?
        [:div.alert.alert-success {:role "alert"}
         [:strong "Success!"] "  Send one more?"])
      [:form {:method "POST"}
       [:div.form-group
        [:input#email-input.form-control {:type "email"
                                          :placeholder "E-mail to..."
                                          :name "to_email"}]]
       [:div.form-group
        [:textarea.form-control {:rows 5
                                 :placeholder "Enter a Message"
                                 :name "message"}]]
       [:div.form-group
        [:label {:for "from-input"} "From"]
        [:input.form-control {:type "text"
                              :placeholder "Enter your name..."
                              :name "from_user"}]]
       [:div.row
        [:div.col-xs-5
         [:div.form-group
          [:label "Send Date"]
          [:input#send-date.form-control {:type "date"
                                          :placeholder "Send Date"
                                          :name "send_date"
                                          :value (to-form-date (java.time.Instant/now))}]]]
        [:div.col-xs-7
         [:div.form-group
          [:label "Add Time"]
          [:div
           [:button.add-days.btn.btn-info.btn-sm {:value 1} "+1 Day"]
           "&nbsp;"
           [:button.add-days.btn.btn-info.btn-sm {:value 7} "+1 Wk"]
           "&nbsp;"
           [:button#reset-date.btn.pull-right.btn-sm "Reset!"]
           ]]]]
       [:br]
       [:input.btn.btn-primary {:type "submit"}]
       [:script {:src "/public/index.js"}]])))

(defn message-page
  [ephemeral]
  (base-template
    (h/html
      [:img {:src (str (:id ephemeral) ".gif")
             :style "display: none"}]
      [:div.well
       [:h5 (:message ephemeral)]
       [:hr]
       [:h6 [:strong "Yours truly, "]]
       [:h6 "&nbsp;&nbsp;&nbsp;&nbsp;" (:from_user ephemeral)]]
      [:div.alert.alert-warning {:role "alert"}
       "This message will be " [:em "deleted"] " after you read this."])))

(def not-found
  (base-template
    (h/html
      [:h4 [:strong.span-error "404!"]" Could not find this page."])))

(defn web-handler
  [db-spec]
  (c/routes
    ;; TODO: would prefer form params but they aren't transformed into keyword params
    (POST "/" {params :form-params}
      (try
        (-> params
          (clojure.walk/keywordize-keys)
          (update :send_date from-form-date)
          (assoc  :id (java.util.UUID/randomUUID))
          (#(db/create! db-spec %)))
        (assoc (ring/redirect "/")
          :flash "created")
        (catch Exception e
          (println e)
          {:status 400
           :body (str "Malformed Request" e)})))

    ;; Endpoint which provides tracking via pixel.
    (GET "/:id.gif" [id]
      (let [row   (db/find-one db-spec id)
            read? (:read row)]
        (when-not (or (nil? read?) read?)
          (->> row
            db/mark-read
            (db/update! db-spec)))
        {:status 200
         :headers {"Content-Type" "image/gif"}
         :body "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEAAAAALAAAAAABAAEAAAI="}))

    (GET "/:id" [id]
      (let [ephemeral (db/find-one db-spec id)]
        (if-not (:read ephemeral false)
          (message-page ephemeral)
          {:status 404
           :body not-found})))

    (GET "/" request
      (homepage (:flash request)))
    (route/resources "/public")
    (route/not-found "No such page.")))

(comment
  ((web-handler db-spec) {:request-method :get
                          :uri "/123"
                          :headers {"Content-Type" "application/x-www-form-urlencoded"}
                          :body "email=chanbessie@gmail.com"}))

(defn start
  [port db-spec]
  (let [handler (-> db-spec
                  web-handler
                  keyword-params/wrap-keyword-params
                  params/wrap-params
                  flash/wrap-flash
                  session/wrap-session
                  stacktrace/wrap-stacktrace)
        server-var (find-var 'ephemeral.web/server)]
    (when (bound? server-var)
      (.close (var-get server-var)))
    (println "Serving on port " port)
    (def server (http/start-server handler {:port port}))))
