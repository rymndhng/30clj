(ns ephemeral.system
  (:require [system.core :refer [defsystem]]
            (system.components
              [aleph :refer [new-web-server]]
              [repl-server :refer [new-repl-server]])
            [environ.core :refer [env]]
            [ephemeral.web :refer [app]]))

(defsystem dev-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))
   :repl-server (new-repl-server (Integer. (env :repl-port)))])
