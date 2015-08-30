(ns ephemeral.email
  (:require [postal.core :as postal]
            [ephemeral.db :as db]))

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
  [[ephemeral & rest] host db-spec mail-auth stop?]
  (let [{:keys [code] :as response} (perform-send ephemeral host mail-auth)]
    (if (= 0 code)
      (mark-ephemeral-read)
      (str "Sending mail failed: " response))

    (when-not @stop?
      (recur rest host db-spec mail-auth stop?))))


(defrecord SendEmailsComponent [host db-url mail-auth]
  component/Lifecycle
  (start [this]

    (println ";; Starting email scheduler")
    (assoc this :)

    )

  )
