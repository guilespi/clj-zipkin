(ns clj-zipkin.tracer
  (:require [thrift-clj.core :as thrift]
            [clojure.data.codec.base64 :as b64]
            [clj-scribe :as scribe]
            [thrift-clj.gen.core :as c]
            [byte-streams]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]))

(thrift/import
  (:types [com.twitter.zipkin.gen 
           Span Annotation BinaryAnnotation AnnotationType Endpoint 
           LogEntry StoreAggregatesException AdjustableRateException])
  (:clients com.twitter.zipkin.gen.ZipkinCollector))

(defn ip-str-to-int [str-ip] 
   (let [nums (vec (map read-string (clojure.string/split str-ip #"\.")))]
      (+ 
         (* (nums 0) 16777216) 
         (* (nums 1) 65536) 
         (* (nums 2) 256) 
         (nums 3))))

(defn str->bytes
  [str]
  (bytes (byte-array (map (comp byte int) str))))

(defn thrift->base64
  "Serializes a thrift span object to be sent through the wires"
  [data]
  (let [buffer (org.apache.thrift.transport.TMemoryBuffer. 40000)
        protocol (org.apache.thrift.protocol.TBinaryProtocol. buffer)
        ;side effectish step
        _ (.write (.to_thrift* data) protocol)]
    (String. (b64/encode (.getArray buffer) 0 (.length buffer)) "UTF-8")))

(defn host->endpoint
  "Given a host in string or map format creates a thrift zipkin endpoint object"
  [host]
  (condp = (class host)
    java.lang.String (Endpoint. (ip-str-to-int host) 0 "Clojure Service")
    clojure.lang.PersistentArrayMap (Endpoint. (ip-str-to-int (:ip host)) 
                                               (or (:port host) 0) 
                                               (or (:service host) "Clojure Service"))
    (throw "Invalid host value")))

(def rand-size 100000)

(defn create-span
  "Creates a new span with start/finish annotations"
  [span host trace-id span-id parent-id start finish]
  (let [endpoint (host->endpoint host)
        start-annotation (Annotation. (* 1000 (time-coerce/to-long start)) (str "start:" (name span)) endpoint 0)
        finish-annotation (Annotation. (* 1000 (time-coerce/to-long finish)) (str "end:" (name span)) endpoint 0)]
    (thrift->base64 
     (Span. trace-id
            (name span) 
            span-id
            parent-id
            [start-annotation
             finish-annotation] 
            []
            0))))

;;tracing macro for nested recording
(def ^:dynamic *trace-id*)
(def ^:dynamic *current-span-id*)
(def ^:dynamic *logger*)

(defmulti parse-item (fn [form] 
                       (if (seq? form) 
                         :seq
                         :default)))

(defmethod parse-item :seq
  [form]
  (if (and (symbol? (first form))
           (= (ns-resolve *ns* (first form)) (ns-resolve 'clj-zipkin.tracer 'trace)))
    (let [[_ data body] form
          span-id (rand rand-size)]
      (list* 'clj-zipkin.tracer/trace* (merge data {:trace-id *trace-id*
                                                    :parent-id *current-span-id*}) (map parse-item body) '()))
    (doall (map parse-item form))))

(defmethod parse-item :default 
  [form]
  form)

(defmacro trace*
  [{:keys [span host trace-id parent-id]} & body]
  (let [span-id (rand rand-size)
        trace-id (or trace-id (rand rand-size))
        body (binding [*trace-id* trace-id
                       *current-span-id* span-id]
               (parse-item body))]
    `(let [start-time# (time/now)
           result# ~@body
           end-time# (time/now)
           _# (scribe/log *logger*
                          [(create-span ~span ~host ~trace-id 
                                        ~span-id ~parent-id
                                        start-time# end-time#)])]
       result#)))

(defmacro trace 
  [& args]
  `(binding [*logger* (scribe/async-logger :host ~(-> args first :scribe :host) 
                                           :port ~(-> args first :scribe :port) 
                                           :category "zipkin")]
     (trace* ~(first args) ~@(rest args))))