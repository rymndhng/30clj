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

                  ;; environment properties
                  [org.danielsz/system "0.1.9"]
                  [environ "1.0.0"]
                  [boot-environ "1.0.0-RC1"]

                  ;; development tools
                  [org.clojure/tools.nrepl "0.2.10"]
                  [ring/ring-devel "1.4.0"]])


(require
  '[reloaded.repl :as repl :refer [start stop go reset]]
  '[ephemeral.system :refer [dev-system prod-system]]
  '[environ.boot :refer [environ]]
  '[environ.core :refer [env]]
  '[system.boot :refer [system run]])

(def dev-env
  (environ :env {:http-host "localhost:3000"
                 :http-port "3000"
                 :db-url "jdbc:postgresql://localhost:5432/ephemerals_dev"}))

(deftask dev
  "Runs a restartable system in the REPL"
  []
  (comp
    dev-env
    (watch :verbose true)
    (system :sys #'dev-system)
    (repl :server true)))

(deftask dev-run
  []
  "Runs a dev system from the command line"
  (comp
    dev-env
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
  (ephemeral.db/ensure-table! (env :db-url))
  identity)
