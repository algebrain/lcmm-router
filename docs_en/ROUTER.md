# API Documentation: Router

This document describes the public API for the `Router` component. The router serves as a central point for registering HTTP routes by all system modules, implementing the Inversion of Control (IoC) principle.

## Constructor

### `make-router`

Creates a new, empty router instance.

```clojure
(require '[lcmm.router :as router])

(def my-router (router/make-router))
```
The constructor does not accept any options. All configuration is done during route registration and compilation.

## Core Functions

### `add-route!`

Registers a single HTTP endpoint (handler) for a specified path and method. This function is thread-safe.

- **Signature:** `(add-route! router method path handler & [opts])`
- **`router`:** The router instance created by `make-router`.
- **`method`:** The HTTP method keyword (e.g., `:get`, `:post`).
- **`path`:** A path string that supports `reitit` syntax (e.g., `"/users/:id"`).
- **`handler`:** A Ring-compliant handler function.
- **`opts`:** (optional) A map with additional `reitit` options, such as:
  - `:name`: The route name (a keyword) for identification.
  - `:middleware`: A vector of Ring middleware applied only to this route.

**Example from a `users` module:**
```clojure
(defn users-module-init!
  "The module's initialization function, receiving dependencies including the router."
  [router]
  (let [find-user-handler (fn [req] {:status 200 :body "user found"})
        create-user-handler (fn [req] {:status 201 :body "user created"})]
    
    (router/add-route! router
                       :get "/api/users/:id"
                       find-user-handler
                       {:name ::find-user})

    (router/add-route! router
                       :post "/api/users"
                       create-user-handler
                       {:name ::create-user
                        :middleware [[my-auth-middleware "admin"]]})))
```

### `as-ring-handler`

Compiles all registered routes into a single, final `Ring-handler`, ready to be run in a web server (e.g., `http-kit`).

- **Signature:** `(as-ring-handler router & [opts])`
- **`router`:** The router instance with all registered routes.
- **`opts`:** (optional) A map with global `reitit` options that will be applied to all routes. Most commonly used to add global `middleware`.

**Important Feature:** The returned handler contains the compiled `reitit` router in its metadata under the `::router/router` key. This is useful for testing and introspection.

**System Startup Example:**
```clojure
(require '[org.httpkit.server :as http-kit])

(defn -main []
  (let [app-router (router/make-router)
        global-middleware [my-logging-middleware my-cors-middleware]]
    
    ;; Modules register their routes
    (users-module-init! app-router)
    (orders-module-init! app-router)
    
    ;; Compile the final handler with global middleware
    (let [http-handler (router/as-ring-handler app-router {:middleware global-middleware})]
      (println "Starting web server on port 8080...")
      (http-kit/run-server http-handler {:port 8080}))))
```

## Testing

To check `reitit`-specific functionality (e.g., finding a route by name), you can extract the compiled router from the `ring-handler`'s metadata.

**Test Example:**
```clojure
(require '[clojure.test :refer :all]
         '[lcmm.router :as router]
         '[reitit.core :as reitit])

(deftest find-by-name-test
  (let [rtr (router/make-router)]
    (router/add-route! rtr :get "/test" (constantly {:status 200}) {:name ::my-test-route})
    
    (let [handler (router/as-ring-handler rtr)
          ;; Extract the router from the metadata
          compiled-router (::router/router (meta handler))] 
      
      (is (some? (reitit/match-by-name compiled-router ::my-test-route))))))
```
