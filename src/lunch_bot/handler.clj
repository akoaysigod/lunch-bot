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

(def ^:private slack-key (System/getenv "SLACKSLASH"))

(defn- verify-key [in-key]
  (if (= slack-key in-key) true false))

(defn- send-response [text]
  (response {:text text :headers {:content-type "application/json"}}))

(defn- handle-request [body]
  (let [user (body "user_name")
        com (first (str/split (body "text") #" "))
        text (rest (str/split (body "text") #" "))]
    (cond
      (= com "random") (yelp/get-random)
      (= com "vote") (vote/start user text)
      :else "What do you want?")))



(defroutes app-routes
  (POST "/lunch" {body :params}
        (if (verify-key (body "token"))
          (handle-request body)
          "Who are you even?"))
  (route/not-found "Not Found"))

(defn- wrap-log-request [handler]
  "console log all the requests"
  (fn [req]
    (println req)
    (handler req)))

(def app
  (-> app-routes
      wrap-log-request
      wrap-params
      wrap-json-response))

