(ns lcmm.router-test
  (:require [clojure.test :refer [deftest is testing]]
            [lcmm.router :as router]
            [reitit.core :as reitit]))

(defn- mock-handler [response]
  (fn [_] response))

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
      
      (let [handler (router/as-ring-handler rtr)
            total-routes (* num-threads routes-per-thread)]
        (is (= total-routes (count (-> rtr :routes-atom deref))))
        (is (= {:status 200} (handler {:request-method :get :uri "/t5/r50"})))
        (is (= 404 (:status (handler {:request-method :get :uri "/t10/r100"}))))))))
