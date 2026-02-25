(ns lcmm.router
  (:require [reitit.ring :as ring]))

(defn- raise!
  [message data]
  (throw (ex-info message data)))

(defn- validate-add-route-args!
  [method path handler opts]
  (when-not (keyword? method)
    (raise! "Route method must be a keyword." {:method method}))
  (when-not (string? path)
    (raise! "Route path must be a string." {:path path}))
  (when-not (ifn? handler)
    (raise! "Route handler must be invokable." {:handler handler}))
  (when-not (map? opts)
    (raise! "Route opts must be a map." {:opts opts})))

(defprotocol IRouter
  "Протокол для роутера, который позволяет добавлять маршруты."
  (add-route! [this method path handler] [this method path handler opts]
    "Регистрирует новый эндпойнт в роутере.
     `opts` - map с ключами :name, :middleware и др. опциями Reitit."))

(defrecord Router [state-atom]
  IRouter
  (add-route! [this method path handler]
    (add-route! this method path handler {}))

  (add-route! [_ method path handler opts]
    (validate-add-route-args! method path handler opts)
    (swap! state-atom
           (fn [{:keys [built? routes] :as state}]
             (when built?
               (raise! "Router is immutable after as-ring-handler call." {}))
             (let [replace?  (true? (:replace? opts))
                   route-opts (dissoc opts :replace?)
                   path-routes (get routes path {})]
               (when (and (contains? path-routes method) (not replace?))
                 (raise! "Route conflict for path and method. Use :replace? true to overwrite."
                         {:path path :method method}))
               (assoc state
                      :routes
                      (update routes path merge (assoc route-opts method handler))))))))

(defn make-router
  "Создает новый экземпляр роутера."
  []
  (->Router (atom {:routes {} :built? false})))

(defn as-ring-handler
  "Компилирует маршруты в Ring-совместимый обработчик.
   Принимает опциональный map с глобальными Reitit-опциями (например, middleware).
   Возвращает Ring-обработчик с Reitit-роутером в метаданных под ключом `::router`."
  ([router]
   (as-ring-handler router {}))
  ([router opts]
   (let [final-routes    (-> (:state-atom router)
                             (swap! assoc :built? true)
                             :routes
                             vec)
         compiled-router (ring/router final-routes opts)]
     (with-meta
       (ring/ring-handler compiled-router
                          (ring/create-default-handler))
       {::router compiled-router}))))
