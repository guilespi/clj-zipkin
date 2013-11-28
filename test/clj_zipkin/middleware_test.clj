(ns clj-zipkin.middleware-test
  (:use clojure.test
        ring.mock.request)
  (:require [clj-zipkin.tracer :as t]
            [clj-zipkin.middleware :as m]
            [clj-scribe :as scribe]
            [clojure.data.codec.base64 :as b64]))

(defn handler 
  [request]
  (:zipkin request))

;;test ring handler properly logs zipkin request
(deftest ring-handler-test
  (let [logger (atom [])
        spans (atom [])]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (swap! logger conj (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! spans (-> args second)))
                  t/thrift->base64 identity]
      (let [response ((m/request-tracer handler {:scribe {:host "localhost" :port 9410}
                                                 :host "10.2.3.5"})
                      (request :get "/sample/url"))
            s1 (first @spans)]
        ;;ids are properly exposed in the chained request
        (is (= response
             {:trace-id (:trace_id s1) :span-id (:id s1)}))
        ;;only one span
        (is (= 1 (count @spans)))
        ;;properly tag start/end annotations with action and uri
        (is (= "start::get:/sample/url" (:value (first (:annotations s1)))))
        (is (= "end::get:/sample/url" (:value (second (:annotations s1)))))
        ;;host properly read from config
        (is (= (t/ip-str-to-int "10.2.3.5") (-> (first (:annotations s1)) :host :ipv4)))))))


;;trace and span id properly retrieved from request headers
(deftest headers-handler-test
  (let [logger (atom [])
        span (atom nil)
        req (request :get "/sample/url")
        headers (:headers req)
        req (assoc req :headers (merge headers {"X-B3-TraceId" 10000
                                                "X-B3-ParentSpanId" 20000
                                                "X-B3-SpanId" 30000}))]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (swap! logger conj (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! span (-> args second first)))
                  t/thrift->base64 identity]
      (let [response ((m/request-tracer handler {:scribe {:host "localhost" :port 9410}
                                                 :service "WebServer"})
                      req)]
        ;;header ids are properly used
        (is (= 10000 (:trace_id @span)) "trace id properly set")
        (is (= 20000 (:parent_id @span)) "parent id properly set")
        (is (= 30000 (:id @span)) "span id properly set")
        ;;service name properly retrieved from configuration
        (is (= "WebServer" (-> (first (:annotations @span)) :host :service_name)) "Service name properly set")))))