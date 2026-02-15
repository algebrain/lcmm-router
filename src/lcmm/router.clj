(ns lcmm.router
  (:require [reitit.ring :as ring]))

(defprotocol IRouter
  "Протокол для роутера, который позволяет добавлять маршруты."
  (add-route! [this method path handler] [this method path handler opts]
    "Регистрирует новый эндпойнт в роутере.
     `opts` - map с ключами :name, :middleware и др. опциями Reitit."))

(defrecord Router [routes-atom]
  IRouter
  (add-route! [_ method path handler]
    (add-route! _ method path handler {}))

  (add-route! [_ method path handler opts]
    (let [route-data (assoc opts method handler)]
      (swap! routes-atom
             (fn [routes]
               (update routes path merge route-data))))))

(defn make-router
  "Создает новый экземпляр роутера."
  []
  (->Router (atom {})))

(defn as-ring-handler
  "Компилирует маршруты в Ring-совместимый обработчик.
   Принимает опциональный map с глобальными Reitit-опциями (например, middleware).
   Возвращает Ring-обработчик с Reitit-роутером в метаданных под ключом `::router`."
  ([router]
   (as-ring-handler router {}))
  ([router opts]
   (let [final-routes    (vec @(:routes-atom router))
         compiled-router (ring/router final-routes opts)]
     (with-meta
       (ring/ring-handler compiled-router
                          (ring/create-default-handler))
       {::router compiled-router}))))
