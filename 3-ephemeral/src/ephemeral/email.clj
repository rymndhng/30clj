(ns ephemeral.email
  (:require [postal.core :as postal]
            [ephemeral.db :as db]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as t]))

(defn template
  [host ephemeral-id]
  (str
    "You received an ephemeral message. Please read it at " host "/" ephemeral-id))

(defn create-email
  [{:keys [id to_email]} ^String host]
  {:from "ephemerals@gmail.com"
   :to [to_email]
   :subject "You received an Ephemeral Message!"
   :body (template host id)})

(defn perform-send
  "Sends an email using postal."
  [ephemeral host mail-auth]
  (->> (create-email ephemeral host)
    (postal/send-message mail-auth)))

(defn mark-ephemeral-read
  [ephemeral db-spec]
  (-> ephemeral
    (assoc :sent true)
    (#(db/update! db-spec %))))

(defn send-emails
  "Sends all emails from a list of ephemerals. Once successfully sent,
  its entry will be updated in the database. If any messages fail to
  send, will throw an exception.  "
  [[ephemeral & rest] host db-spec mail-auth]
  (when-not (or (nil? ephemeral) (Thread/interrupted))
    (let [{:keys [code] :as response} (perform-send ephemeral host mail-auth)]
      (if (= 0 code)
        (mark-ephemeral-read ephemeral db-spec)
        ;; TODO: make printing work
        (t/error "Sending mail failed: " response))
      (recur rest host db-spec mail-auth))))

(defrecord SendEmailsComponent [server-name db-url mail-auth]
  component/Lifecycle
  (start [component]
    (t/info ";; Starting emails worker")
    (if (:future component)
      component
      (let [stop (promise)
            future (future (-> (db/unread-mails db-url 10 5000 stop)
                             (send-emails server-name db-url mail-auth)))]
        (merge component {:future future
                          :stop stop}))))

  (stop [component]
    (t/info ";; Stopping email worker")
    (if-not (:future component)
      component
      (do
        (when (deliver (:stop component) :stopped)
          (try
            @(:future component)
            (catch Exception e (println e))))
        (merge component {:future nil
                          :stop nil})))))

(defn get-config
  "Gets the auth mail configuration"
  [type host user pass]
  (condp = type
    "smtp" {:host host
            :user user
            :pass pass
            :ssl :yes}
    {}))

(defn new-send-email [server-name db-url mail-auth]
  (map->SendEmailsComponent {:server-name server-name :db-url db-url :mail-auth mail-auth}))
