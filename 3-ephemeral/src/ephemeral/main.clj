(ns ephemeral.mail
  (:gen-class)
  (:require
   [reloaded.repl :refer [system init start stop go reset]]
   [ephemeral.system :refer [prod-system]]))

(defn -main
  "Starts a production system."
  (let [system (or (first args) #'prod-system)]
    (reloaded.repl/set-init! system)
    (go)))
