(ns lunch-bot.handler
  (:use ring.middleware.json)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [response]]
            [clojure.string :as str]
            [lunch-bot.yelp :as yelp]
            [lunch-bot.vote :as vote]))

(def ^:private slack-key (System/getenv "SLACKOUTGOING"))

(defn- verify-key [in-key]
  (if (= slack-key in-key) true false))

(defn- send-response [text]
  (response {:text text :headers {:content-type "application/json"}}))

(defn- handle-request [body]
  (let [user (body "user_name")
        com (first (rest (str/split (body "text") #" ")))
        text (rest (rest (str/split (body "text") #" ")))]
    (cond
      (= com "random") (send-response (yelp/get-random))
      (= com "vote") (send-response (vote/start user text))
      :else (send-response "What do you want?"))))



(defroutes app-routes
  (POST "/lunch" {body :form-params}
        (if (verify-key (body "token"))
          (handle-request body)
          (send-response "Who are you even?")))
  (route/not-found "Not Found"))

(defn- wrap-log-request [handler]
  "console log all the requests"
  (fn [req]
    (println req)
    (handler req)))

(def app
  (-> app-routes
;;      wrap-log-request
      wrap-params
      wrap-json-response))

