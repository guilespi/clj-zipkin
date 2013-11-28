(ns examples.server
  (:require [ring.adapter.jetty :as jetty]
            [compojure.route :as route]
            [clj-zipkin.middleware :as m]
            [clj-zipkin.tracer :as t])
  (:use [compojure.core :only (GET PUT POST ANY defroutes context)]))


(def config {:scribe {:host "localhost" :port 9410}
             :service "TestService"})

(defn main-renderer
  [request]
  ;;functions traced during request processing
  ;;can take zipking parameters from request                                      
  (t/trace {:span "Renderer" 
            ;;this values should be propagated over the network in
            ;;case execution continues somewhere else
            :trace-id (get-in request [:zipkin :trace-id])
            :parent-span-id (get-in request [:zipkin :span-id])
            ;specify only service name, default other parameters
            :host {:service "RendererService"}
            :scribe (:scribe config)}
           (do
             (Thread/sleep 100)
             "<h1>Hello World</h1>")))

(defroutes routes
  (GET "/" request (main-renderer request))
  (route/not-found "<h1>Page not found</h1>"))

(def app (-> routes 
             (m/request-tracer config)))

(def server (atom nil))

(defn stop []
  (when @server
    (.stop @server)))

(defn start []
  (stop)
  (when-let [s (jetty/run-jetty app 
                                {:join? false :port 9090})]
    (reset! server s)))
