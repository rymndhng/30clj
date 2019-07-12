(ns batch_writer.core
  (:import [java.util.concurrent TimeUnit BlockingQueue]))

;; (set! *warn-on-reflection* true)

(defprotocol BatchEventPublisher
  "A user-defined publisher.

  - cost: should compute the cost of an event
  - above-watermark?: when true, will invoke publish on the messages without the event crossing the watermark
  - publish: a function to publish the batch events"
  ;; if the new entry puts us above the watermark
  ;; if so, messages will be published *without* the message
  (cost [_ event])
  (above-watermark? [_ costs])
  (publish [_ events]))

(defn new-batcher [publisher]
  {:events []
   :costs []
   :publisher publisher})

(defn collect [batcher event cost]
  (println "collecting")
  (-> batcher
      (update :events conj event)
      (update :costs conj cost)))

(defn collect-and-publish [{:keys [publisher events costs] :as batcher} event]
  (let [cost (cost publisher event)]
    (if (above-watermark? publisher (conj costs cost))
      (do
        (publish publisher events)
        (collect (new-batcher publisher) event cost))
      (collect batcher event cost))))

(defn background-worker [^BlockingQueue queue {:keys [wait-time publisher]}]
  (loop [queue   queue
         batcher (new-message-batcher publisher)]
    (let [message (try
                    (.poll queue wait-time TimeUnit/MILLISECONDS)
                    (catch InterruptedException ex
                      (println "thread halted -- flushing before shutting down")
                      (publish publisher (:events batcher))
                      (throw ex)))]
      (if message
        (do
          (println "received message, processing ...")
          (recur queue (collect-and-publish batcher message)))
        (do
          (println "no message recieved, flushing ...")
          (publish publisher (:events batcher))
          (recur queue (new-message-batcher publisher)))))))


;; -- New Fun
(def payload-max-bytes 14)

(defn event-message-publisher []
  (reify BatchEventPublisher
    (cost [_ event]
      (alength (.getBytes (str event))))
    (above-watermark? [_ costs]
      (let [value (apply + costs)]
        (println "at watermark " value)
        (> value payload-max-bytes)))
    (publish [_ events]
      (if (empty? events)
        (println "no events received ... skipping ")
        (println "writing batch" {:events events})))))

(comment
  (def ^java.util.concurrent.LinkedBlockingQueue queue (java.util.concurrent.LinkedBlockingQueue.))
  (def publisher (event-message-publisher))
  (def worker-fut (future
                    (try
                      (background-worker queue {:publisher publisher :wait-time 10000})
                      (catch Exception ex
                        (println ex)
                        (throw ex)))))

  (dotimes [n 10]
    (.offer queue :foo))

  worker-fut
  (future-cancel worker-fut)
  )
