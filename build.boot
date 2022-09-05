(set-env!
  :resource-paths #{"src" "tst"}
  :dependencies   '[[org.clojure/clojure "1.8.0"  :scope "provided"]
                    [adzerk/bootlaces    "0.1.13" :scope "test"]
                    [adzerk/boot-test    "1.1.2"  :scope "test"]])

(ns-unmap 'boot.user 'test)

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.bootlaces :refer :all])

;;; configs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +version+ "3.0.0-SNAPSHOT")
(bootlaces! +version+)

;;; tasks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask develop []
  (comp (watch) (speak) (build-jar)))

(deftask deploy []
  (comp (speak) (build-jar) (push-release)))

(task-options!
  pom  {:project     'jumblerg/ring-cors
        :version     +version+
        :description "Simple Ring middleware for easy cross-origin resource sharing (CORS)."
        :url         "https://github.com/jumblerg/ring-cors"
        :scm         {:url "https://github.com/jumblerg/ring-cors"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}}
  test {:namespaces  ['jumblerg.middleware.cors-test]})
