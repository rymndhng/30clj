(ns ephemeral.system
  (:require [system.core :refer [defsystem]]
            (system.components
              [aleph :refer [new-web-server]]
              [repl-server :refer [new-repl-server]])
            [environ.core :refer [env]]
            [schema.core :as s]
            (ephemeral
              [web :refer [app]]
              [email :refer [new-send-email] :as email])))

;; Configurations are a pain, so get it right from the start.
(s/defschema STMPConfig
  {:host s/Str
   :user s/Str
   :pass s/Str
   :ssl s/Keyword})

(s/defschema Configuration
  {:http-host s/Str
   :http-port s/Int
   :db-url    s/Str
   :mail-auth (s/either (s/eq {}) STMPConfig)
   (s/optional-key :repl-port) s/Int
   s/Any s/Any})

(defsystem dev-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))
   :worker (new-send-email
             (env :http-host)
             (env :db-url)
             (env :mail-auth))])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port))
          (app (env :db-url)))
   :worker (new-send-email
             (env :http-host)
             (env :db-url)
             (env :mail-auth))
   :repl-server (new-repl-server (Integer. (env :repl-port)))])
