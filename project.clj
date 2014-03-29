(defproject clj-zipkin "0.1.3"
  :description "Zipkin tracing instrumentation for Clojure applications."
  :url "https://github.com/guilespi/clj-zipkin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [thrift-clj "0.2.1"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.clojure/data.codec "0.1.0"]
                 [clj-scribe "0.3.1"]
                 [clj-time "0.6.0"]]
  :test-paths ["test" "examples"]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [compojure "1.1.6"]
                                  [ring "1.2.1"]]}}
  :plugins [[lein-thriftc "0.1.0"]]
  :prep-tasks ["thriftc"]
  :javac-options ["-Xlint:unchecked"])
