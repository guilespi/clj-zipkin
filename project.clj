(defproject clj-zipkin "0.1.1"
  :description "Zipkin tracing instrumentation for Clojure applications."
  :url "https://github.com/guilespi/clj-zipkin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [thrift-clj "0.1.1"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.clojure/data.codec "0.1.0"]
                 [clj-scribe "0.3.1"]
                 [byte-streams "0.1.6"] 
                 [clj-time "0.6.0"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :plugins [[lein-thriftc "0.1.0"]]
  :prep-tasks ["thriftc"]
  :javac-options ["-Xlint:unchecked"])
