(ns ring.middleware.cors-test
  (:require
  	[clojure.test :refer :all]
    [ring.middleware.cors :refer :all] ))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-header          [res hdr] (-> res :headers (get hdr)))
(defn contains-header?    [res hdr] (-> res :headers (contains? hdr)))
(def  not-contain-header? (complement contains-header?))

;;; tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest test-wrap-cors

  (testing 
    "if the origin header is not present terminate."
    (let [req {:headers        {} 
               :request-method :get 
               :uri            "/"}
          res  {:status        200}
          ret ((wrap-cors (fn [_] res) #"http://authorizedorigin.com") req)]
      (is (not (get-header ret "Access-Control-Allow-Origin"))) ))

  (testing 
    "if the value of the origin header is not a case-sensitive match for any of the values 
    in list of origins do not set any additional headers and terminate."
    (let [req-hdrs {"origin" "http://uauthorizedorigin.com"}
          req      {:headers        req-hdrs 
                    :request-method :get 
                    :uri            "/"}
          res      {:status         200}
          ret      ((wrap-cors (fn [_] ) #"http://authorizedorigin.com") req)]
      (is (not (get-header ret "Access-Control-Allow-Origin"))) ))

  (testing
    "if the resource supports credentials add a single Access-Control-Allow-Origin header with 
    the value of the Origin header as value and add a single Access-Control-Allow-Credentials 
    header with the case-sensitive string \"true\" as value. (in this case, the resource supports
    credentials only when the client requests them either by the presence of the authorization 
    header)"
    (let [req-hdrs {"origin"        "http://authorizedorigin.com" 
                    "authorization" "Basic Y2hyaXNAZXhhbXBsZS5jb206YmVlcno="}
          req      {:headers        req-hdrs      
                    :request-method :get 
                    :uri            "/"}
          res      {:status         200}
          ret      ((wrap-cors (fn [_] res) #"http://authorizedorigin.com") req)]
      (is (= "http://authorizedorigin.com" (get-header ret "Access-Control-Allow-Origin")))
      (is (= "true"                        (get-header ret "Access-Control-Allow-Credentials"))) ))

  (testing 
    "multiple wildcard origins"
    (let [req-hdrs {"origin"        "http://authorizedorigin.com"}
          req      {:headers        req-hdrs      
                    :request-method :get 
                    :uri            "/"}
          res      {:status         200}
          ret      ((wrap-cors (fn [_] res) #".*authorizedorigin.com" #".*example.com") req)]
      (is (= "http://authorizedorigin.com" (get-header ret "Access-Control-Allow-Origin"))) 
      (is (= "true"                        (get-header ret "Access-Control-Allow-Credentials"))) ))

  (testing 
    "if the list of exposed headers is not empty add one or more Access-Control-Expose-Headers
     with the header field names given in the list of exposed headers as values."
    (let [req-hdrs {"origin"           "http://authorizedorigin.com" 
                    "authorization"    "Basic Y2hyaXNAZXhhbXBsZS5jb206YmVlcno="}
          req      {:headers           req-hdrs      
                    :request-method    :get 
                    :uri               "/"}
          res-hdrs {"Content-Type"     "application/json"
                    "Set-Cookie"       "Example=Zm9vYmFyYmF6;Max-Age=3600;Path=/"
                    "WWW-Authenticate" "Basic realm=\"example\""}
          res      {:headers           res-hdrs
                    :status            200}
          ret      ((wrap-cors (fn [_] res) #"http://authorizedorigin.com") req)]
      (is (= "http://authorizedorigin.com"  (get-header ret "Access-Control-Allow-Origin")))
      (is (= "true"                         (get-header ret "Access-Control-Allow-Credentials")))
      (is (= "WWW-Authenticate, Set-Cookie" (get-header ret "Access-Control-Expose-Headers"))) ))

  (testing 
    "if the origin header is not present terminate the preflight."
    (let [req-hdrs {"access-control-request-method"  "GET"
                    "access-control-request-headers" "accept, remember"}
          req      {:headers                         req-hdrs 
                    :request-method                  :options 
                    :uri                             "/"}
          res      {:status                          200}
          ret ((wrap-cors (fn [_] res) #"http://authorizedorigin.com") req)]
      (is (not (get-header ret "Access-Control-Allow-Origin"))) ))
 
  (testing 
    "if the value of the origin header is not a case-sensitive match for any of the values 
    in list of origins do not set any additional headers and terminate"
    (let [req-hdrs {"origin"                         "http://uauthorizedorigin.com"
                    "access-control-request-method"  "GET"
                    "access-control-request-headers" "accept, remember"}
          req      {:headers                         req-hdrs 
                    :request-method                  :options 
                    :uri                             "/"}
          res      {:status                          200}
          ret      ((wrap-cors (fn [_] ) #"http://authorizedorigin.com") req)]
      (is (not (get-header ret "Access-Control-Allow-Origin"))) ))

  (testing
    "if there is no Access-Control-Request-Method header or if parsing failed, do not set any 
    additional headers and terminate this set of steps."
    (let [req-hdrs {"origin"        "http://uauthorizedorigin.com"}
          req      {:headers        req-hdrs 
                    :request-method :options 
                    :uri            "/"}
          res      {:status         200}
          ret      ((wrap-cors (fn [_] ) #"http://authorizedorigin.com") req)]
      (is (not (get-header ret "Access-Control-Allow-Origin"))) ))

  (testing
    "if the resource supports credentials add a single Access-Control-Allow-Origin header, with 
    the value of the Origin header as value, and add a single Access-Control-Allow-Credentials 
    header with the case-sensitive string \"true\" as value. (in this case, the resource supports
    credentials only when the client requests them by the presence of authorization in the access
    control request headers)"
    (let [req-hdrs {"origin"                         "http://authorizedorigin.com" 
                    "access-control-request-method"  "GET"
                    "access-control-request-headers" "accept, remember, authorization"}
          req      {:headers                         req-hdrs      
                    :request-method                  :options
                    :uri                             "/"}
          res      {:status                          200}
          ret      ((wrap-cors (fn [_] res) identity) req)]
      (is (= (ret :status) 200))
      (is (= "http://authorizedorigin.com"     (get-header ret "Access-Control-Allow-Origin")))
      (is (= "true"                            (get-header ret "Access-Control-Allow-Credentials"))) 
      (is (= "GET"                             (get-header ret "Access-Control-Allow-Methods")))
      (is (= "accept, remember, authorization" (get-header ret "Access-Control-Allow-Headers")))
      (is (= "86400"                           (get-header ret "Access-Control-Max-Age"))) 
      (is (not-contain-header? ret "Access-Control-Expose-Headers")) )))