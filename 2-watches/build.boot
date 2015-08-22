#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  ;; TODO: what is an asset path
  :dependencies '[[aleph "0.4.0"]
                  [compojure "1.4.0"]
                  [hiccup "1.0.5"]

                  ;; clojurescripts
                  [adzerk/boot-cljs "1.7.48-2"]])

(require '[adzerk.boot-cljs :refer :all])
(require '[server :as s])

(defn -main [& args]
  "Runs the server in a daemon! Aha!"
  (boot (cljs))
  (s/-main)
  (agent {}))
