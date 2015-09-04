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
            [ring.middleware.content-type :as content-type]
            [ring.middleware.proxy-headers :as proxy-headers]
            [ring.util.response :as response]
            [taoensso.timbre :as t]
            [schema.core :as schema]))

(defn to-form-date
  "How to serialize into a date input."
  [^java.time.Instant date]
  (when date
    (-> (java.time.format.DateTimeFormatter/ISO_LOCAL_DATE)
      (.withZone (java.time.ZoneId/systemDefault))
      (.format date))))

(defn ^java.time.Instant from-form-date
  "If you give me a long one, I will shorten it"
  [text]
  (if (empty? text)
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
      [:base {:src "~/"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:link {:rel "stylesheet"
              :href "public/paper.bootstrap.min.css"}]
      [:body
       [:div.container
        [:div.row
         [:div.col-xs-12.col-sm-2.col-md-3]
         [:div.col-xs-12.col-sm-8.col-md-6
          [:h1 "Ephemeral Messages"]
          content
          [:a {:href "."} "Send your own Ephemeral."]
          [:footer
           "Made by " [:a {:href "www.twitter.com/rymndhng"} "@rymndhng"]]]]]]]]))

(defn error-to-text
  "Returns human readable text for an error string. If none present,
  simply returns the key."
  [err]
  (condp = err
      :invalid-email "Invalid Email!"
      :length-greater-than-zero "This field should not be empty."
      :valid-instant "Invalid Date!"
      err))

(defn form-group
  "Creates a Boostrap form group for conditionally showing error text.

  schema-error  Primsatic/Schema error binded to the form element.
  elements      Hiccup form element(s) to wrap.
  "
  ([error & elements]
   ;; TODO: rework this more elegantly. Need to read the schema API more deeply.
   (if-let [error-key (condp = (type error)
                        schema.utils.ValidationError
                        (some-> error .schema :pred-name)

                        schema.utils.NamedError
                        (some-> error .name)

                        nil)]
     `[:div.form-group.has-error
       ~@elements
       [:span.help-block ~(error-to-text error-key)]]

     `[:div.form-group
       ~@elements])))

(defn homepage
  [{:keys [created? errors ephemeral]}]
  (base-template
    (h/html
      [:h5 "Send an Ephemeral to a loved one!"]
      (when created?
        [:div.alert.alert-success {:role "alert"}
         [:strong "Success!"] "  Send one more?"])
      [:form {:method "POST"}
       (form-group (:to_email errors)
         [:input#email-input.form-control {:type "email"
                                           :placeholder "E-mail to..."
                                           :name "to_email"
                                           :value (:to_email ephemeral "")}])
       (form-group (:message errors)
         [:textarea.form-control {:rows 5
                                  :placeholder "Enter a Message"
                                  :name "message"
                                  :value (:message ephemeral "")}])

       (form-group (:from_user errors)
         [:label {:for "from-input"} "From"]
         [:input.form-control {:type "text"
                               :placeholder "Enter your name..."
                               :name "from_user"
                               :value (:from_user ephemeral "")}])
       [:div.row
        [:div.col-xs-5
         (form-group (:send_date errors)
           [:label "Send Date"]
           [:input#send-date.form-control {:type "date"
                                           :placeholder "Send Date"
                                           :name "send_date"
                                           :value (-> (:send_date
                                                       ephemeral
                                                       (java.time.Instant/now))
                                                    to-form-date)}])]
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
      [:h4 [:strong.span-error "404!"] " Could not find this page."])))

(def unexpected-error
  (base-template
    (h/html
      [:h4 [:string.span-error "Error!"]
       "Not sure what happened. Maybe file an "
       [:a {:href "https://github.com/rymndhng/30clj/issues"} "issue" ]
       '.])))

(defn web-handler
  [server-name db-spec]
  (c/routes
    (POST "/" {params :form-params}
      (let [model (-> params
                    (clojure.walk/keywordize-keys)
                    (update :send_date from-form-date)
                    (assoc  :id (java.util.UUID/randomUUID)))]
        (if-let [errors (schema/check db/Ephemeral model)]
          (-> {:status 400
               :body (homepage {:errors errors
                                :ephemeral model})}
            (response/content-type "text/html"))
          (try
            (db/create! db-spec model)
            (t/spy (assoc (response/redirect server-name :see-other)
                     :flash "created"))
            (catch Exception e
              (println e)
              (-> {:status 400
                   :body unexpected-error}))))))

    ;; Endpoint which provides tracking via pixel.
    (GET "/:id.gif" [id]
      (let [row   (db/find-one db-spec id)
            read? (:read row)]
        (when-not (or (nil? read?) read?)
          (->> row
            db/mark-read
            (db/update! db-spec)))
        (-> "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEAAAAALAAAAAABAAEAAAI="
          response/response
          (response/content-type "image/gif"))))

    (GET "/:id" [id]
      (let [ephemeral (db/find-one db-spec id)]
        (if-not (:read ephemeral true)
          (message-page ephemeral)
          (-> (response/not-found not-found)
            (response/content-type "text/html")))))

    (GET "/" request
      (homepage {:created? (:flash request)}))
    (route/resources "/public")
    (route/not-found "No such page.")))

(comment
  ((web-handler db-spec) {:request-method :get
                          :uri "/123"
                          :headers {"Content-Type"
                                    "application/x-www-form-urlencoded"}
                          :body "email=chanbessie@gmail.com"}))

(defn app
  [server-name db-spec]
  (-> (web-handler server-name db-spec)
    keyword-params/wrap-keyword-params
    params/wrap-params
    flash/wrap-flash
    session/wrap-session
    content-type/wrap-content-type
    proxy-headers/wrap-forwarded-remote-addr
    stacktrace/wrap-stacktrace))
