#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.3.1"

(def config (read-string (slurp "config.edn")))

(apply set-env! (:main config))
(add-sync! (get-env :out-path) (get-env :rsc-paths))

(refer-clojure :exclude [test])
(require
  '[clojure.test                   :refer [run-tests]]
  '[tailrecursion.boot.task.notify :refer [hear]]
  '[ring.middleware.cors-test] )

(deftask with-profile
  "Setup build for the given profile from `config.edn`."
  [profile]
  (apply set-env! (get config profile))
  (add-sync! (get-env :out-path) (get-env :rsc-paths))
  identity )

(deftask reload
  "Reload the changed files"
  [& namespaces]
  (fn [continue]
    (fn [event]
      ;(apply require namespaces :reload)
      (require 'ring.middleware.cors      :reload)
      (require 'ring.middleware.cors-test :reload)
      (continue event) )))

(deftask test
  "Run unit tests."
  [& namespaces]
  (fn [continue]
    (fn [event]
      (apply run-tests namespaces)
      (continue event) )))

(deftask develop
  "Run the unit tests each time the source changes."
  []
  (comp (watch) (hear) (test 'ring.middleware.cors-test) (reload 'ring.middleware.cors-test 'ring.middleware.cors)) )