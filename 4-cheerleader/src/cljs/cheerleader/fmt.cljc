(ns cheerleader.fmt)

(defn readable-key
  [key]
  (as-> (name key) %
    (clojure.string/split % #"-")
    (map clojure.string/capitalize %)
    (clojure.string/join " " %)))

(defn ratio-to-css-pct [ratio]
  (-> ratio
    (* 100)
    str
    (subs 0 10)
    (str "%")))
