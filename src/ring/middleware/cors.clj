(ns ring.middleware.cors
  (:require 
    [clojure.string :refer [join]] ))

;;; constants ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def simple-hdrs
  ["Cache-Control"
   "Content-Language"
   "Content-Type"
   "Expires" 
   "Last-Modified"
   "Pragma" ])

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro guard [& body] `(try ~@body (catch Throwable _#)))

(defn clean [map] (into {} (remove (comp nil? val) map)))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cors-hdrs [req-hmap]
  (let [orig  (req-hmap "origin")]
    {"Access-Control-Allow-Origin"      orig
     "Access-Control-Allow-Credentials" "true" }))

(defn pflt-hdrs [req-hmap]
  (let [hstr (req-hmap "access-control-request-headers")
        mthd (req-hmap "access-control-request-method") 
        secs (str (* 60 60 24))]
    {"Access-Control-Max-Age"       secs
     "Access-Control-Allow-Headers" hstr
     "Access-Control-Allow-Methods" mthd }))

(defn stnd-hdrs [{res-hmap :headers}]
  (let [hvec (apply dissoc res-hmap simple-hdrs)
        hstr (->> hvec keys (join ", ")) ]
    {"Access-Control-Expose-Headers" hstr} ))

(defn allow-origins [req handler & [allow :as orig-regxps]]
  (let [req-hmap (req :headers)
        req-orig (req-hmap "origin")
        allow-fn #(some (fn [x] (guard (re-matches x %1))) %2)
        mergeh   #(update-in %1 [:headers] (comp clean merge) (cors-hdrs req-hmap) %2)
        cors-ok? (if (fn? allow) (allow req-orig) (allow-fn req-orig orig-regxps))
        pflt-ok? (and cors-ok? (= (get req :request-method) :options))]
    (let [resp (handler req)]
      (cond pflt-ok? (-> resp (assoc :status 200) (mergeh (pflt-hdrs req-hmap))) 
            cors-ok? (->> resp ((juxt identity stnd-hdrs)) (apply mergeh))
            :else    resp))))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-cors [handler & allowed-origins]
  "Ring task for enabling cross-origin resource sharing (CORS) where allowed-origins is a list of 
  regular expressions matching the permitted origin(s) or a single function which takes the origin 
  as its argument to return a truthy value if it is to be allowed."
  (fn [req] (apply allow-origins req handler allowed-origins) ))
