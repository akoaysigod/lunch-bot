(ns lunch-bot.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [lunch-bot.handler :refer :all]))

(def token (System/getenv "SLACKOUTGOING"))




(deftest test-app
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(def nonsense-req {"user_name" "user" "text" "lunch nonsense" "token" token})
(deftest test-nonsense
  (testing "nonsense"
    (let [response (app (mock/request :post "/lunch" nonsense-req))]
      (is
       (=
        (response :body)
        "{\"text\":\"What do you want?\",\"headers\":{\"content-type\":\"application/json\"}}")))))

(def random-req {"user_name" "user" "text" "lunch random" "token" token})
(deftest random-test
  (testing "random"
    (let [response (app (mock/request :post "/lunch" random-req))]
      (is (= (:status response) 200)))))

