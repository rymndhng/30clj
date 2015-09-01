(ns ephemeral.system
  (:require [system.core :refer [defsystem]]
            (system.components
              [aleph :refer [new-web-server]]
              [repl-server :refer [new-repl-server]])
            [environ.core :refer [env]]
            (ephemeral
              [web :refer [app]]
              [email :refer [new-send-email] :as email])))

(defsystem dev-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))
   :worker (new-send-email
             (env :http-host)
             (env :db-url)
             (email/get-config
               (env :mail-type)
               (env :mail-host)
               (env :mail-user)
               (env :mail-pass)))])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))
   :worker (new-send-email
             (env :http-host)
             (env :db-url)
             (email/get-config
               (env :mail-type)
               (env :mail-host)
               (env :mail-user)
               (env :mail-pass)))
   :repl-server (new-repl-server (Integer. (env :repl-port)))])
