(ns ephemeral.core-test
  (:require [ephemeral.core :as e]
            [schema.core :as s]
            [clojure.test :refer [deftest is testing] :as t]))

(deftest schema-checks
  (testing "Valid Cases"
    (testing "Minimal Example"
      (s/validate e/Ephemeral {:id (java.util.UUID/randomUUID)
                               :from_user "Ray"
                               :to_user   "Bess"
                               :to_email "b@chan.com"
                               :message   "Hi!"
                               :send_date (java.time.Instant/now)}))
    (testing "Full Example"
      (s/validate e/Ephemeral {:id (java.util.UUID/randomUUID)
                               :from_user "Ray"
                               :to_user   "Bess"
                               :to_email "b@chan.com"
                               :message   "Hi!"
                               :send_date (java.time.Instant/now)
                               :sent      true
                               :read      true
                               :received_time (java.time.Instant/now)})))
  (testing "Invalid Cases"
    (testing "extra keys"
      (is (thrown? Exception
            (s/validate e/Ephemeral {:id (java.util.UUID/randomUUID)
                                     :from_user "Ray"
                                     :to_user   "Bess"
                                     :to_email "b@chan.com"
                                     :message   "Hi!"
                                     :send_date (java.time.Instant/now)
                                     :extra-key "LOLOL"}))))
    (testing "Wrong Types"
      (is (thrown? Exception
            (s/validate e/Ephemeral {:id (java.util.UUID/randomUUID)
                                     :from_user "Ray"
                                     :to_user   "Bess"
                                     :to_email "b@chan.com"
                                     :message   "Hi!"
                                     :send_date "2015-07-04"}))))))


(def postgres
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/ephemeral_test"})

(def sqlite
  {:subprotocol "sqlite"
   :classname   "org.sqlite.JDBC"
   :subname     "database.sqlite"})

(deftest crud-integration
  (let [db-spec postgres
        before {:id (java.util.UUID/randomUUID)
                :from_user "Ray"
                :to_user   "Bess"
                :to_email "b@chan.com"
                :message   "Hi!"
                :send_date (java.time.Instant/now)}
        after (assoc before :message "Boo!")
        result (merge after {:sent false
                             :read false
                             :received_time nil})]
    (e/ensure-table! db-spec)
    (e/create! db-spec before)
    (is (= 1 (e/update! db-spec after)))
    (is (= result (e/find-one db-spec (:id result))))))

(t/run-tests)
