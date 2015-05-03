(ns lunch-bot.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [lunch-bot.handler :refer :all]))

(def ^:private token (System/getenv "SLACKSLASH"))




(deftest test-app
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (response :status) 404)))))

(def no-token-req {"user_name" "tony" "text" "whatever" "token" "w123asd"})
(deftest no-token
  (testing "no token"
    (let [response (app (mock/request :post "/lunch" no-token-req))]
      (is (= (response :body) "Who are you even?")))))

(def nonsense-req {"user_name" "user" "text" "nonsense" "token" token})
(deftest test-nonsense
  (testing "nonsense"
    (let [response (app (mock/request :post "/lunch" nonsense-req))]
      (is (= (response :body) "What do you want?")))))

(def random-req {"user_name" "user" "text" "random" "token" token})
(deftest random-test
  (testing "random"
    (let [response (app (mock/request :post "/lunch" random-req))]
      (is (= (response :status) 200)))))

(def vote-req {"user_name" "user" "text" "vote first" "token" token})
(deftest vote-test
  (testing "first vote"
    (let [response (app (mock/request :post "/lunch" vote-req))]
      (is (= (response :status) 200))
      (is (= (response :body) "Yay, you voted.")))))
