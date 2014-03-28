(ns clj-zipkin.tls
  (:require [clj-zipkin.tracer :as tracer]
            [clj-time.core :as time])
  (:import (clojure.lang IDeref)))


(defn- thread-local*
  "Thread local storage clojure impl, taken from useful:
   https://github.com/flatland/useful/blob/develop/src/flatland/useful/utils.clj"
  [init]
  (let [generator (proxy [ThreadLocal] []
                    (initialValue [] (init)))]
    (reify IDeref
      (deref [this]
        (.get generator)))))

(defmacro thread-local
  [& body]
  `(thread-local* (fn [] ~@body)))

(def ^{:doc "Span being traced on the current thread"} 
  thread-local-span (thread-local (atom nil)))

(defn start-span 
  "Starts a new span using the thread local storage
   as state.

   When start-span has been called it's possible to:
   * Add new annotations to current span with add-annotation
   * Close and record the current span with close-span
   * Cancel de current span with clear-span"
  [{:keys [operation host trace-id parent-id] :as span}]
  (reset! @thread-local-span {:operation operation
                              :host host
                              :trace-id trace-id
                              :span-id (tracer/create-id)
                              :parent-id parent-id
                              :annotations []
                              :start-time (time/now)})
  @@thread-local-span)

(defn add-annotation
  "Adds annotation to the thread local span"
  [annotation]
  (when @@thread-local-span
    (swap! @thread-local-span update-in [:annotations] conj annotation)))

(defn get-span
  "Retrieves the current thread span from TLS."
  []
  @@thread-local-span)

(defn clear-span
  "Clears the current thread span from TLS without logging."
  []
  (reset! @thread-local-span nil))

(defn close-span
  "Finishes the current thread span and logs using scribe connection, 
   clears current-span afterwards."
  [connection]
  (when-let [span @@thread-local-span]
    (let [span-list [(tracer/create-timestamp-span (:operation span) 
                                                   (:host span)
                                                   (:trace-id span)
                                                   (:span-id span)  
                                                   (:parent-id span)
                                                   (:start-time span) 
                                                   (time/now))]]
      (tracer/log connection span-list)
      (clear-span))))
