(ns jumblerg.middleware.cors
  (:require
    [clojure.string :refer [join]]))

;;; constants ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def simple-hdrs
  ["Cache-Control"
   "Content-Language"
   "Content-Type"
   "Expires"
   "Last-Modified"
   "Pragma"])

;;; utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro guard [& body] `(try ~@body (catch Throwable _#)))

(defn clean [map] (into {} (remove (comp nil? val) map)))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cors-hdrs [req-hmap]
  (let [orig (req-hmap "origin")]
    {"Access-Control-Allow-Origin"      orig
     "Access-Control-Allow-Credentials" "true"}))

(defn pflt-hdrs [req-hmap]
  (let [hstr (req-hmap "access-control-request-headers")
        mthd (req-hmap "access-control-request-method")
        secs (str (* 60 60 24))]
    {"Access-Control-Max-Age"       secs
     "Access-Control-Allow-Headers" hstr
     "Access-Control-Allow-Methods" mthd}))

(defn stnd-hdrs [{res-hmap :headers}]
  (let [hvec (apply dissoc res-hmap simple-hdrs)
        hstr (->> hvec keys (join ", "))]
    {"Access-Control-Expose-Headers" hstr}))

(defn allow-origins [req handle allow]
  (let [req-hdrs   (req :headers)
        merge-hdrs #(update %1 :headers (comp clean merge) (cors-hdrs req-hdrs) %2)]
    (if (allow (req-hdrs "origin"))
      (if (contains? req-hdrs "access-control-request-method")
        (merge-hdrs {:status 204} (pflt-hdrs req-hdrs))
        (apply merge-hdrs ((juxt identity stnd-hdrs) (handle req))))
      (handle req))))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-cors [handle & [allow :as allowed-origins]]
  "ring task for enabling cross-origin resource sharing (CORS) where allowed-
   origins is either a predicate function that takes the origin as its argument
   or a list of regular expressions matching the permitted origin(s)."
  (let [allow (if (fn? allow) allow #(some (fn [x] (guard (re-matches x %1))) allowed-origins))]
    (fn [req] (allow-origins req handle allow))))
