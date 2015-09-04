#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[aleph "0.4.0"]
                  [compojure "1.4.0"]
                  [hiccup "1.0.5"]
                  [ring/ring-headers "0.1.3"]

                  [org.clojure/java.jdbc "0.4.1"]
                  [org.postgresql/postgresql "9.4-1201-jdbc41"]
                  [prismatic/schema "1.0.1"]

                  ;; email and core.async for background processing
                  [com.draines/postal "1.11.3"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                  ;; environment properties
                  [org.danielsz/system "0.1.9"]
                  [environ "1.0.0"]
                  [boot-environ "1.0.0-RC1"]
                  [com.taoensso/timbre "4.1.1"]

                  ;; development tools
                  [org.clojure/tools.nrepl "0.2.10"]
                  [ring/ring-devel "1.4.0"]])

(require
  '[reloaded.repl :as repl :refer [start stop go reset]]
  '[ephemeral.system :refer [dev-system prod-system Configuration]]
  '[environ.core :as environ]
  '[schema.core :as s]
  '[system.boot :refer [system run]])

(def dev-config
  "We print a string to re-use the generic reader. Sigh."
  {:server-name "localhost:3000"
   :http-port 3000
   :db-url "jdbc:postgresql://localhost:5432/ephemerals_dev"
   :mail-auth {}})

(deftask dev-env
  "Merges a map environment -- because I like to do that in a REPL."
  []
  (with-redefs [environ/env (merge environ/env (s/validate Configuration dev-config))]
    identity))

(deftask dev
  "Runs a restartable system in the REPL"
  []
  (comp
    (dev-env)
    (watch :verbose true)
    (system :sys #'dev-system)
    (repl :server true)))

(deftask dev-run
  []
  "Runs a dev system from the command line"
  (comp
    (dev-env)
    (run :main-namespace "ephemeral.main" :arguments [#'dev-system])
    (wait)))

(deftask prod-run
  []
  (comp
    (run :main-namespace "ephemeral.main" :arguments [#'prod-system])
    (wait)))

(deftask build
  "Builds an uberjar that can be run"
  []
  (comp
    (aot :namespace '#{ephemeral.main})
    (pom :project 'ephemeral
         :version "1.0.0")
    (uber)
    (jar :main 'ephemeral.main)))

(deftask migrate
  "Performs a database migration."
  []
  (ephemeral.db/ensure-table! (environ/env :db-url))
  identity)
