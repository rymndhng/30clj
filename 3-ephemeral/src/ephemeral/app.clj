;; This is where all the magic happens
(ns ephemeral.app
  (:require [ephemeral.db :as db]
            [ephemeral.email :as email]
            [ephemeral.web :as web]))

;; TODO: set up environment

(def default-db-spec
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/ephemerals"})

(def default-mail-spec
  {})

;; Move the bootstrapping here

(defn -main
  []
  (web/start 2000 default-db-spec)
  ;; Starts sending emails on the main thread. What's the elegant way of doing
  ;; this?
  (->> (db/unread-mails default-db-spec 10 10000)
    (email/send-emails
      "localhost:2000"
      default-db-spec
      default-mail-spec)))

#_(-main)
