(ns demo.build-boot
  (:require [boot.core :as core]
            [boot.task.built-in :as task]))

(core/deftask build
  "Print a friendly greeting."
  []
  (comp (task/pom (task/jar) (task/install))))
