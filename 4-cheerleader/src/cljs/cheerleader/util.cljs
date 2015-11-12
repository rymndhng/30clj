(ns cheerleader.util)

(defn elapsed [start end]
  "Gets the elapsed milliseconds from start date to end date with a period of
  milliseconds. "
  (-> (- (.getTime end) (.getTime start))))
