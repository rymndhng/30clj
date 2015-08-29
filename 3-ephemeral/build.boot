#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[aleph "0.4.0"]
                  [compojure "1.4.0"]
                  [hiccup "1.0.5"]

                  [org.clojure/java.jdbc "0.4.1"]
                  [org.postgresql/postgresql "9.4-1201-jdbc41"]
                  [prismatic/schema "0.4.4"]

                  ;; email and core.async for background processing
                  [com.draines/postal "1.11.3"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                  ;; this is for development
                  [ring/ring-devel "1.4.0"]])

(require '[ephemeral.core :as core])

;; (deftask migrate
;;   "Runs database migrations."
;;   []
;;   (core/ensure-table! default-db-spec)
;;   identity)

;; (deftask run
;;   "Start the server in a background thread."
;;   []
;;   (let [start (delay (future (app/-main [])))]
;;     (with-pre-wrap @start)))
