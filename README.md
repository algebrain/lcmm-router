# LCMM Router

[![Clojars Project](https://img.shields.io/clojars/v/lcmm/router.svg)](https://clojars.org/lcmm/router)

`lcmm/router` is a lightweight, centralized routing component for Clojure applications, designed to support a loosely coupled, modular monolith architecture. It implements an Inversion of Control (IoC) pattern, allowing independent modules to register their own HTTP routes on a shared router instance.

The core idea is that the application's entry point creates a single router instance and passes it to each module during initialization. The modules use the router to define their endpoints. Finally, the main application compiles all registered routes into a single [Ring-compliant](https://github.com/ring-clojure/ring) handler.

This project uses [`reitit`](https://github.com/metosin/reitit) under the hood for powerful and fast routing.

## Quick Start

First, add the library to your `deps.edn`:
```clojure
;; deps.edn
lcmm/router {:mvn/version "..."}
```

Next, create a router, register routes, and compile a Ring handler:
```clojure
(require '[lcmm.router :as router]
         '[org.httpkit.server :as http-kit])

;; 1. Create a central router
(def app-router (router/make-router))

;; 2. Pass it to modules to register routes
(router/add-route! app-router :get "/api/health"
  (fn [_] {:status 200 :body "OK"})
  {:name ::health-check})

;; 3. Compile the final handler for your web server
(def http-handler (router/as-ring-handler app-router))

;; (http-kit/run-server http-handler {:port 8080})
```

## Documentation

- **[ARCH.md](./docs/ARCH.md):** For a high-level overview of the modular architecture this component is designed for.
- **[ROUTER.md](./docs/ROUTER.md):** For a detailed API reference, usage examples, and testing guidelines.

## License

Copyright © 2024 Your Name

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License, version 2
with the GNU Classpath Exception which is available at
https://www.gnu.org/software/classpath/license.html.
