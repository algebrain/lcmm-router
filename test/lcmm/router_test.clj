(ns lcmm.router-test
  (:require [clojure.test :refer [deftest is testing]]
            [lcmm.router :as router]
            [reitit.core :as reitit]))

(defn- mock-handler [response]
  (fn [_] response))

(defn- exception-message-matches?
  [pattern f]
  (try
    (f)
    false
    (catch clojure.lang.ExceptionInfo e
      (boolean (re-find pattern (ex-message e))))))

(deftest basic-routing-test
  (testing "Регистрация и вызов простого GET маршрута"
    (let [rtr (router/make-router)
          get-response {:status 200 :body "GET OK"}
          post-response {:status 201 :body "POST OK"}]
      (router/add-route! rtr :get "/test" (mock-handler get-response))
      (router/add-route! rtr :post "/test" (mock-handler post-response))

      (let [handler (router/as-ring-handler rtr)]
        (is (= get-response (handler {:request-method :get :uri "/test"})))
        (is (= post-response (handler {:request-method :post :uri "/test"})))
        (is (= 404 (:status (handler {:request-method :get :uri "/not-found"}))))))))

(deftest route-with-opts-test
  (testing "Регистрация маршрута с опциями (имя маршрута)"
    (let [rtr (router/make-router)]
      (router/add-route! rtr :get "/named" (mock-handler {:status 200}) {:name ::my-route})
      (let [handler (router/as-ring-handler rtr)
            ring-router (::router/router (meta handler))
            match (reitit.core/match-by-name ring-router ::my-route)]
        (is (= "/named" (:path match)))))))

(deftest concurrent-registration-test
  (testing "Конкурентная регистрация маршрутов из разных потоков"
    (let [rtr (router/make-router)
          num-threads 10
          routes-per-thread 100]
      (->> (range num-threads)
           (mapcat (fn [thread-id]
                     (map (fn [route-id]
                            #(router/add-route! rtr :get (str "/t" thread-id "/r" route-id) (constantly {:status 200})))
                          (range routes-per-thread))))
           (mapv #(doto (Thread. ^Runnable %) (.start)))
           (run! #(.join ^Thread %)))

      (let [handler (router/as-ring-handler rtr)]
        (is (= {:status 200} (handler {:request-method :get :uri "/t5/r50"})))
        (is (= 404 (:status (handler {:request-method :get :uri "/t10/r100"}))))))))

(deftest duplicate-route-fails-by-default-test
  (testing "Конфликт path+method по умолчанию приводит к ошибке"
    (let [rtr (router/make-router)]
      (router/add-route! rtr :get "/dup" (mock-handler {:status 200 :body "v1"}))
      (is (exception-message-matches?
           #"Route conflict"
           #(router/add-route! rtr :get "/dup" (mock-handler {:status 200 :body "v2"})))))))

(deftest duplicate-route-can-be-replaced-explicitly-test
  (testing "Конфликт path+method можно разрешить только через :replace? true"
    (let [rtr (router/make-router)
          first-response {:status 200 :body "v1"}
          second-response {:status 200 :body "v2"}]
      (router/add-route! rtr :get "/dup-replace" (mock-handler first-response))
      (router/add-route! rtr :get "/dup-replace" (mock-handler second-response) {:replace? true})
      (let [handler (router/as-ring-handler rtr)]
        (is (= second-response
               (handler {:request-method :get :uri "/dup-replace"})))))))

(deftest add-route-validation-test
  (testing "add-route! валидирует method/path/handler/opts"
    (let [rtr (router/make-router)
          handler (mock-handler {:status 200})]
      (is (exception-message-matches?
           #"method must be a keyword"
           #(router/add-route! rtr "GET" "/invalid-method" handler)))
      (is (exception-message-matches?
           #"path must be a string"
           #(router/add-route! rtr :get :invalid-path handler)))
      (is (exception-message-matches?
           #"handler must be invokable"
           #(router/add-route! rtr :get "/invalid-handler" 42)))
      (is (exception-message-matches?
           #"opts must be a map"
           #(router/add-route! rtr :get "/invalid-opts" handler []))))))

(deftest router-freeze-after-build-test
  (testing "После as-ring-handler роутер становится immutable"
    (let [rtr (router/make-router)]
      (router/add-route! rtr :get "/before-build" (mock-handler {:status 200}))
      (router/as-ring-handler rtr)
      (is (exception-message-matches?
           #"immutable after as-ring-handler"
           #(router/add-route! rtr :get "/after-build" (mock-handler {:status 200})))))))
