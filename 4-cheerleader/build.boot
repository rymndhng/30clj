(set-env!
 :source-paths    #{"src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.170-3"      :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.0"          :scope "test"]
                 [adzerk/boot-reload        "0.4.2"          :scope "test"]
                 [pandeiro/boot-http        "0.7.1-SNAPSHOT" :scope "test"]

                 ;; needed for dev
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [weasel "0.7.0" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]

                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]])

(deftask build []
  (comp (speak)
        (cljs)
        ))

(deftask run []
  (comp (serve)
        (watch)
        (cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :compiler-options {:asset-path "js/app.out"}
                       :source-map true}
                 reload {:on-jsload 'cheerleader.app/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))
