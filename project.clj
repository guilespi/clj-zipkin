(defproject clj-zipkin "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [thrift-clj "0.1.1"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.clojure/data.codec "0.1.0"]
                 [clj-scribe "0.3.1"]
                 [byte-streams "0.1.6"] 
                 [clj-time "0.6.0"]]
  :plugins [[lein-thriftc "0.1.0"]]
  :prep-tasks ["thriftc"]
  :javac-options ["-Xlint:unchecked"])
