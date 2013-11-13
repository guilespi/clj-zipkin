# clj-zipkin

Zipkin tracing instrumentation for Clojure applications.

## Usage

Add the following dependency to your `project.clj` file:

       [clj-zipkin "0.1.0"]

### Tracing

Tracing a code chunk 

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :scribe {:host "zipkin.host" :port 9410}}
         (..code..))

```

Nested tracing, scribe config can be avoided for inner tracing. This will make inner traces
 appear in zipkin as childs of the immediate parent trace.

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :scribe {:host "zipkin.host" :port 9410}}
         (..code..)
         (t/trace {:host "10.2.1.2" :span "OTHER"}
                  (..code..)))

```

Tracing with a specific `trace-id`, since all related spans in zipkin should share the
same `trace-id`, if your code is executing in different hosts you can tie your traces
together specifying the `trace-id` to use.

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :trace-id 12345 :scribe {:host "zipkin.host" :port 9410}}
         (..code..)
         (t/trace {:host "10.2.1.2" :span "OTHER"}
                  (..code..)))

```

** Parameters **

Tracing parameters

```clojure
  :host => current host, defaults to InetAddress/getLocalHost if unspecified
  :span => span name
  :trace-id => optional, new one will be created if unspecified
  :scribe => scribe/zipkin endpoint configuration {:host h :port p}
```


### Ring Handler

Work in progress

## TODO

* Default hostname to current host
* Ring handler

## License

Copyright © 2013 Guillermo Winkler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
