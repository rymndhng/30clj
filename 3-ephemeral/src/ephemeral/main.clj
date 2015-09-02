(ns ephemeral.main
  (:gen-class)
  (:require
   [reloaded.repl :refer [system init start stop go reset]]
   [schema.core :as schema]
   [environ.core :as environ]
   [ephemeral.system :refer [prod-system Configuration]]))

(defn -main
  [& args]
  "Starts a production system."
  (schema/validate Configuration environ/env)
  (let [system (or (first args) #'prod-system)]
    (reloaded.repl/set-init! system)
    (go)))
