# clj-zipkin

Zipkin tracing instrumentation for Clojure applications.

## Usage

Add the following dependency to your `project.clj` file:

       [clj-zipkin "0.1.3"]

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

#### Parameters

Tracing parameters

```
  :host => current host, defaults to InetAddress/getLocalHost if unspecified
  :span => span name
  :trace-id => optional, new one will be created if unspecified
  :span-id => optional, current span-id, useful to append annotations or to keep track of created id
  :parent-span-id => optional, parent span-id, this span will be nested
  :scribe => scribe/zipkin endpoint configuration {:host h :port p}
```

The `host` parameter can also be a structured hash-map

```clojure
   {:host "10.2.1.1" :port 3030 :service "Service Name"}
```

If not specified will default to port `0` and service `Unknown Service`.

#### Connection

The `make-logger` function creates a logger object to be reused among different tracing calls.

Use it in order to avoid creation of a new connection for each logged span.

```clojure

(def conn (t/make-logger {:host "zipkin.host" :port 9410}))

(t/trace {:host "10.2.1.2" :span "GET" :scribe conn}
         (..code..))

```
### Thread based tracing

In the particular case you can't or really don't want to wrap your code in the `trace` macro call, there's a set of apis using [Thread Local Storage][1] with the `start` and `close` span api calls decoupled.

Require the namespace `tls`, for thread local storage, or thread local spans.

```clojure
(:require [clj-zipkin.tracer :as t]
          [clj-zipkin.tls :as tls])
```

**Start span**

At your logging entry point create the new span, this operation doesn't log a thing to zipkin just yet.

```clojure
;;this should happen at entry point
(tls/start-span {:operation "GET" 
                 :host "10.2.1.1" 
                 :trace-id trace-id
                 :parent-id parent-id})
```

There's no need to keep track of state or variables since it's stored as thread state. 

**Propagation** 

If somewhere inside your traced code you need to retrieve the current trace or span ids - to propagate to other services for instance -, use the `get-span` api call.

```clojure
(:trace-id (tls/get-span))
=> 12345
```

The variables `trace-id` and `span-id` are available for proper trace propagation.

**Annotations**

If some information is collected during the operation, it's possible to append annotations to the thread local span using the `add-annotation` api call. `add-annotation` receives a map with name/values to annotate.

```clojure
(tls/add-annotation {:n1 1 :n2 "da"})
```

**Closing span**

When span is finished and ready to be logged to zipkin, issue a `close-span` call passing a logger connection parameter.

```
(tls/close-span (t/make-logger {:host "localhost" :port 9410}))
```

### Ring Handler

A ring handler is available for automated tracing of incoming requests

```clojure
   (require '[clj-zipkin.middleware :as m])
  
   (defroutes routes
     (GET "/" [] "<h1>Hello World</h1>")
     (route/not-found "<h1>Page not found</h1>"))

   (def app
       (-> routes
       (m/request-tracer {:scribe {:host "localhost" :port 9410}
                          :service "WebServer"})))
   
```

Configuration received by the tracer handler is almost the same as the `trace` function defined above, so it also supports a `host` parameter.

```clojure
(m/request-tracer {:scribe {:host "localhost" :port 9410}
                   :host {:port 2020 :service "MyService"}}))
```

If no `ip` is specified will default to `java.net.InetAddress/getLocalHost`

##Example

There's a small running example [here](examples/server.clj) showing how to chain different spans in a web server.

This is how it looks that example in zipkin:

![zipkin sample](doc/images/clj-zipkin-sample.png?raw=true)


##TODO

* Binary Annotations
* Dynamic Annotations support in the API
* Change span annotations default text?

## License

Copyright © 2013 Guillermo Winkler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: http://docs.oracle.com/javase/7/docs/api/java/lang/ThreadLocal.html