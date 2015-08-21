;; This is day 1
(set-env!
  :source-paths #{"src"}
  :dependencies '[[me.raynes/conch "0.8.0"]])

(task-options!
  pom {:project 'my-project
       :version "0.1.0"}
  jar {:manifest {"Foo" "bar"}})

(require '[demo.build-boot :refer :all])

(boot (pom) (jar) (install))

(boot (pom :version "0.1.1") (jar) (install))
