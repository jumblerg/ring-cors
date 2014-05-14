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

(defn stnd-hdrs [res-hmap]
  (let [hvec (apply dissoc res-hmap simple-hdrs)
        hstr (->> hvec keys (join ", ")) ]
    {"Access-Control-Expose-Headers" hstr} ))

(defn allow-origins [req handler & [allow :as orig-regxps]]
  (let [req-hmap (req :headers)
        req-orig (req-hmap "origin")
        allow-fn #(some (fn [x] (re-matches x %1)) %2)
        allow?   #(if (fn? allow) (allow %1) (allow-fn %1 orig-regxps))
        preflt?  #(= (get % :request-method) :options)
        mergeh   #(update-in %1 [:headers] (comp clean merge) (cors-hdrs req-hmap) %2) ]
    (cond 
      (nil? req-orig)
        {:status 403 :headers {} :body "No Origin Header Specified on Cross-Origin Request"}
      (nil? (allow? req-orig))
        {:status 403 :headers {} :body (str "Origin " req-orig " Not Permitted on Cross-Origin Request")}
      (preflt? req)
        (cond
          (nil? (req-hmap "access-control-request-method"))
            {:status 403 :headers {} :body "No Access-Control-Request-Method Specified on COR Preflight"}
          (nil? (req-hmap "access-control-request-headers"))
            {:status 403 :headers {} :body "No Access-Control-Request-Headers Specified on COR Preflight"}
          :else
            (-> (handler req) (assoc :status 200) (mergeh (pflt-hdrs req-hmap))) )
      :else 
        (let [ret (handler req)] (mergeh ret (stnd-hdrs (ret :headers)))) )))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-cors [handler & allowed-origins]
  "Ring task for enabling cross-origin resource sharing (CORS) where allowed-origins is a list of 
  regular expressions matching the permitted origin(s) or a single function which takes the origin 
  as its argument to return a truthy value if it is to be allowed."
  (fn [req] (apply allow-origins req handler allowed-origins) ))