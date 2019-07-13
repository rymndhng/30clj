(ns batch_writer.json_size
  "Demonstrate the ability to calculate the size of a JSON payload with a variable array of entries.

  Assuming messages is a variable list, we can calculate the final message size with:

  (size of envelope) + sum(size of messages + padding)

  where padding = 1 for the comma separator."

  ;; cheshire is a clojure wrapper for Jackson
  (:require [cheshire.core :as json]))

(def message {"id"       "E16B0A78-AE51-4B7D-8BCC-2E1C3FB983A1",
              "access"   {"response_size" 5125,
                          "response_time" 55555},
              "metadata" {"request_entity_body" "alskdfjalsdkjflaskjdflaksjdfalsdfjalskdfj",
                          "request_headers"     ["Foo Bar", "Baz Qux", "lol Foo"],
                          "request_method"      "GET",
                          "request_path"        "foasldkfjaskdfj/sdf/asd/fasd/fjasldfjalskdfj",
                          "request_version"     "HTTP/1.1",
                          "response_code"       200,
                          "response_headers"    ["Foo Bar", "Baz Qux", "lol Foo"],
                          "socket_addres"       1234}})

;; this is a baseline comparison, assume we have a message with some fixed payload size
(-> message json/encode .getBytes alength)
434

(defn envelope-with-messages [n]
  {"id"       "E16B0A78-AE51-4B7D-8BCC-2E1C3FB983A1"
   "metadata" {}
   "messages" (repeat n message)})


(-> (envelope-with-messages 0) json/encode .getBytes alength)
73
;; an empty message will have a fixed size of 73

(-> (envelope-with-messages 1) json/encode .getBytes alength)
507
;; cost is: 73 + 434

(-> (envelope-with-messages 2) json/encode .getBytes alength)
942
;; cost is 73 + 434 + (434 + 1) for a comma

(-> (envelope-with-messages 3) json/encode .getBytes alength)
1377
;; cost is 73 + 434 + (434 + 1) + (434 + 1)
