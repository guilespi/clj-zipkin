(ns clj-zipkin.tls-test
  (:use clojure.test)
  (:require [clj-zipkin.tracer :as t]
            [clj-zipkin.tls :as tls]
            [clj-scribe :as scribe]
            [clojure.data.codec.base64 :as b64]))

(comment

  (tls/start-span {:operation "GET" 
                   :host "10.2.1.1" 
                   :trace-id 20191912
                   ;:parent-id 010101
                   })

  (tls/get-span)

  (tls/add-annotation {:some 1 :values "da"})

  (tls/close-span (t/make-logger {:host "localhost" :port 9410}))
)

