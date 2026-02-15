# Router Component Implementation for a Modular Monolith

## 1. Goal

To create a centralized route management component (`Router`) that allows independent system modules to register their endpoints (`handlers`) without direct dependencies on each other. The result of the router's work must be a standard `Ring-handler`.

## 2. Architectural Principles

- **Inversion of Control (IoC):** Modules declare their own routes using a provided router instance.
- **Thread Safety:** Route registration must be thread-safe, as modules can be initialized in parallel.
- **Ring Compliance:** All handlers must comply with the Ring specification.
- **Low Coupling:** The router must not have knowledge of the internal logic of the modules.

## 3. Key Functions (Interface)

The router must provide the following Application Programming Interface (API):

### A. Route Registration

A function (or method) that accepts:

- **HTTP method:** `:get`, `:post`, `:put`, `:delete`, etc.
- **Path:** A string or pattern, e.g., `"/users/:id"`.
- **Handler function:** A function that accepts a `request` and returns a `response`.
- **(Optional) A map of options,** which can contain:
  - `:name` — a route name for link generation.
  - `:middleware` — a vector of middleware for a specific route.

### B. Compilation (Build)

A function that transforms the collected route data into a final `ring-handler`. After this function is called, no further changes should be made to the router (immutable state after initialization).

## 4. Recommended Data Structure and Library

It is recommended to use the **Reitit** library, as it is the de-facto standard in Clojure, offers high performance, and supports the separation of route description and compilation.

- **Storage:** Use an `atom` containing a `map`, where keys are paths and values are `map`'s with route descriptions (`{:name :... :get ...}`). This solves the problem of duplicate paths.
- **Protocol (optional):** Define an `IRouter` protocol for ease of testing and substitution.

## 5. Implementation Example (for the implementer)

```clojure
(ns component.router
  (:require [reitit.ring :as ring]))

(defprotocol IRouter
  (add-route! [this method path handler] [this method path handler opts]
    "Registers a new endpoint in the router.
     `opts` - a map with keys like :name, :middleware, and other Reitit options."))

(defrecord Router [routes-atom]
  IRouter
  (add-route! [this method path handler]
    (add-route! this method path handler {}))
  
  (add-route! [this method path handler opts]
    (let [route-data (assoc opts method handler)]
      (swap! routes-atom
             (fn [routes]
               (update routes path merge route-data))))))

(defn make-router
  "Creates a new router instance."
  []
  (->Router (atom {})))

(defn as-ring-handler
  "Compiles routes into a Ring-compatible handler.
   Accepts an optional map of global Reitit options (e.g., middleware)."
  ([router]
   (as-ring-handler router {}))
  ([router opts]
   (let [final-routes (->> @(:routes-atom router)
                           (mapv (fn [[path data]] (into [path] (vals data)))))]
     (ring/ring-handler
       (ring/router final-routes opts)
       (ring/create-default-handler)))))
```

## 6. Lifecycle in the System

1.  **Initialization:** An instance of `(make-router)` is created in the main file (`system/main`).
2.  **Registration:** The instance is passed to the initialization function of each module:
    ```clojure
    (module.orders/init! router)
    (module.users/init! router)
    ```
3.  **Startup:** The main controlling module calls `(as-ring-handler router)` and passes the result to `http-kit/run-server`.

## 7. Implementation Requirements

- **Middleware Support:** The router must allow adding middleware both globally (via `as-ring-handler` options) and at the level of a specific route (via `opts` in `add-route!`).
- **Conflicts:** When registering a route with an existing method and path, `reitit` will, by default, overwrite the old handler. The proposed `add-route!` implementation with `merge` follows this behavior. This is the expected behavior for an IoC container.
- **Documentation:** Every public function must be provided with a `docstring`.
- **Tests:** Cover concurrent route registration from different threads with tests.
