(ns ephemeral.email
  (:require [postal.core :as postal]
            [ephemeral.db :as db]
            [com.stuartsierra.component :as component]))

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
        (str "Sending mail failed: " response))
      (recur rest host db-spec mail-auth))))


(defrecord SendEmailsComponent [host db-url mail-auth]
  component/Lifecycle
  (start [component]
    (println ";; Starting emails worker")
    (if (:worker component)
      component
      (assoc component :email-worker (future (-> (db/find-unsent db-url 10)
                                         (send-emails host db-url mail-auth))))))

  (stop [component]
    (println ";; Stopping email worker")
    (if-not (:email-worker component)
      component
      (do
        (future-cancel (:worker component))
        @(:worker component) ;; should wait til thy end
        (assoc component :email-worker nil)))))

(defn new-send-email [{:keys [host db-url mail-auth]}]
  (map->SendEmailsComponent {:host host :db-url db-url :mail-auth mail-auth}))
