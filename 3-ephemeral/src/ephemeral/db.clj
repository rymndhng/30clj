(ns ephemeral.db
  (:require [clojure.java.jdbc :as j]
            [schema.core :as s]))

(defn ensure-table!
  [db-spec]
  (j/db-do-commands db-spec true
    "DROP TABLE IF EXISTS ephemerals"
    (j/create-table-ddl :ephemerals
      [:id        "uuid"      "PRIMARY KEY"]
      [:from_user "varchar(200)" "NOT NULL"]
      [:to_email  "varchar(200)" "NOT NULL"]
      [:message   "varchar(500)" "NOT NULL"]
      [:send_date "timestamp"     "NOT NULL"]
      [:sent      "boolean"      "DEFAULT false"]
      [:read      "boolean"      "DEFAULT false"]
      [:received_time "timestamp"]))
  "CREATE INDEX ephemeral_send_date ON ephemerals (sent, send_date)")

(defn from-db
  "PostgreSQL Adapter for Deserialization."
  [row]
  (-> row
    (update :send_date #(java.time.Instant/ofEpochMilli (.getTime %)))
    (update :received_time #(when % (java.time.Instant/ofEpochMilli (.getTime %))))))

(defn to-db
  "PostgreSQL Adapter for Serialization."
  [model]
  (-> model
    (update :send_date #(java.sql.Timestamp. (.toEpochMilli %)))
    (update :received_time #(when % (java.sql.Timestamp. (.toEpochMilli %))))))

(def NonZeroString
  (s/both
    s/Str
    (s/pred (fn [x] (pos? (count x))) :length-greater-than-zero)))

(def ValidEmail
  "Really simple email validator"
  (s/both
    NonZeroString
    (s/pred (fn [x] (.contains x "@")) :invalid-email)))

(def Instant
  (s/named java.time.Instant :valid-instant))

;; TODO: explore an ORM that can maybe generate the CRUD for us :)
(def Ephemeral
  "A schema for a nested data type"
  {:id s/Uuid
   :from_user NonZeroString
   :to_email ValidEmail
   :message NonZeroString
   :send_date Instant
   (s/optional-key :sent) s/Bool
   (s/optional-key :read) s/Bool
   (s/optional-key :received_time) (s/maybe java.time.Instant)})

(defn create!
  "Validates one then adds it! Returns the ID of the inserted object"
  [db-spec model]
  (s/validate Ephemeral model)
  (j/insert! db-spec :ephemerals (to-db model)))

(defn update!
  "Updates thy model"
  [db-spec model]
  (s/validate Ephemeral model)
  (first (j/update! db-spec :ephemerals (to-db model) ["id = ?" (:id model)])))

(defn mark-read
  "Sets the ephemeral as read."
  [model]
  (merge model {:read true
                :received_time (java.time.Instant/now)}))

(defn sql-now
  []
  (-> (java.util.Date.)
    .getTime
    java.sql.Timestamp.))

(defn find-unsent
  "Executes thy business logic"
  [db-spec limit]
  (j/query db-spec ["SELECT * FROM ephemerals WHERE sent = false AND send_date <= ? LIMIT ?"
                    (sql-now) limit]
    :row-fn from-db))

(defn find-one
  "Finds an Ephemeral which is not yet read."
  [db-spec id]
  (first (j/query db-spec ["SELECT * from ephemerals WHERE id::text = ?" id]
           :row-fn from-db)))

(defn unread-mails
  "Returns a lazy sequence of unsent emails. Employs a wait-time
  between chunks to throttle reads to the database.

  db-spec     clojure.jdbc connection
  batch-size  number of elements (max) to fetch from SQL Query
  wait-time   number of milliseconds to wait between successive queries
  stop?       promise/future whose resolution stops this infinite stream
 "
  [db-spec batch-size wait-time ^clojure.lang.IPending stop?]
  (lazy-cat
    (find-unsent db-spec batch-size)
    (when-not (realized? stop?)
      (Thread/sleep wait-time)
      (unread-mails db-spec batch-size wait-time stop?))))
