(ns clj-zipkin.middleware
  (:require [clj-zipkin.tracer :as t]))

(defn request-tracer [handler config]
  (fn [request]
    (let [tid (or (get (:headers request) "X-B3-TraceId") (t/create-id))
          pid (get (:headers request) "X-B3-ParentSpanId")
          sid (or (get (:headers request) "X-B3-SpanId") (t/create-id))]
      (t/trace {:trace-id tid
                :span-id sid
                :parent-span-id pid
                :span (str (:request-method request) ":" (:uri request))
                :host (or (:host config) 
                          (when (:service config) 
                            {:service (:service config)}))
                :scribe (:scribe config)}
               (handler (assoc request :zipkin {:trace-id tid 
                                                :span-id sid}))))))

