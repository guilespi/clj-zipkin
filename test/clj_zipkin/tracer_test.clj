(ns clj-zipkin.tracer-test
  (:use clojure.test)
  (:require [clj-zipkin.tracer :as t]
            [clj-scribe :as scribe]
            [clojure.data.codec.base64 :as b64]))

;;test basic tracing parameters
(deftest basic-trace-test 
  (let [logger (atom nil)
        span (atom nil)]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (reset! logger (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! span (-> args second first)))
                  t/thrift->base64 identity]
      (is 2 (t/trace {:host "1.1.1.1" 
                      :span "test-span"
                      :scribe {:host "localhost" :port 9410}} 
                     (+ 1 1)))
      (is (= {:host "localhost" :port 9410 :category "zipkin"}
             @logger))
      (is (= "test-span" (:name @span)))
      (is (nil? (:parent_id @span)))
      (is (= 2 (count (:annotations @span))))
      (is (= "start:test-span" (:value (first (:annotations @span)))))
      (is (= (t/ip-str-to-int "1.1.1.1") (-> (first (:annotations @span)) :host :ipv4)))
      (is (= 0 (-> (first (:annotations @span)) :host :port)))
      (is (= "Unknown Service" (-> (first (:annotations @span)) :host :service_name)))
      (is (= "end:test-span" (:value (second (:annotations @span))))))))

;;test if structured hosts are logged with all the info
(deftest structured-host-trace-test 
  (let [logger (atom nil)
        span (atom nil)]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (reset! logger (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! span (-> args second first)))
                  t/thrift->base64 identity]
      (is 2 (t/trace {:host {:ip "1.1.1.1" :port 8080 :service "ServiceName"}  
                      :span "test-span"
                      :scribe {:host "localhost" :port 9410}} 
                     (+ 1 1)))
      (is (= 2 (count (:annotations @span))))
      (is (= (t/ip-str-to-int "1.1.1.1") (-> (first (:annotations @span)) :host :ipv4)))
      (is (= 8080 (-> (first (:annotations @span)) :host :port)))
      (is (= "ServiceName" (-> (first (:annotations @span)) :host :service_name))))))

;;test if nested transactions properly take the parent-id of the parent transaction
;;and only one logger instance is created
(deftest nested-trace-test
  (let [logger (atom [])
        spans (atom [])]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (swap! logger conj (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! spans (-> args second)))
                  t/thrift->base64 identity]
      (is (= 5 (t/trace {:host "10.10.2.1"
                         :span "nested-test-parent"
                         :scribe {:host "localhost" :port 9410}}
                        (+ 1 (t/trace {:span "nested-test-child"}
                                      (+ 2 2))))))
      ;;nested traces only create one logger
      (is (= 1 (count @logger)))
      ;;should have two spans now
      (is (= 2 (count @spans)))
      ;;first span is always the parent
      (is (nil? (:parent_id (first @spans))))
      ;;second span parent id is first span id
      (is (= (:parent_id (second @spans)) (:id (first @spans)))))))

;;test if hardcoded ids passed by paremeter are properly used
(deftest wired-ids-trace-test 
  (let [logger (atom nil)
        span (atom nil)]
    (with-redefs [scribe/async-logger (fn [& args] 
                                        (reset! logger (apply hash-map args)))
                  scribe/log (fn [& args] 
                               (reset! span (-> args second first)))
                  t/thrift->base64 identity]
      (is 2 (t/trace {:host "1.1.1.1" 
                      :span "test-span"
                      :trace-id 000001
                      :span-id 000002
                      :parent-span-id 101010
                      :scribe {:host "localhost" :port 9410}} 
                     (+ 1 1)))
      (is (= 101010 (:parent_id @span)))
      (is (= 000001 (:trace_id @span)))
      (is (= 000002 (:id @span))))))

