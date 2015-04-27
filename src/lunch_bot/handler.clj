(ns lunch-bot.handler
  (:use ring.middleware.json)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [response]]
            [clojure.string :as str]
            [gws.yelp.client :as client]
            [gws.yelp.api :as api]))

(def slack-key (System/getenv "SLACKOUTGOING"))
(def yelp-key (System/getenv "YELP_CONSUMER_KEY"))
(def yelp-consumer-secret (System/getenv "YELP_CONSUMER_SECRET"))
(def yelp-token (System/getenv "YELP_TOKEN"))
(def yelp-token-secret (System/getenv "YELP_TOKEN_SECRET"))

(def yelp-client (client/create yelp-key yelp-consumer-secret yelp-token yelp-token-secret))

(defn verify-key [in-key]
  (if (= slack-key in-key) true false))

(defn send-response [text]
  (response {:text text :headers {:content-type "application/json"}}))

(defn handle-request [body]
  (let [user (body "user_name")
        text (rest (str/split (body "text") #" "))]
  (cond
    (= text "random") (send-response "Yay!")
    :else (send-response "What do you want?")
    )))

(defroutes app-routes
  (POST "/lunch" {body :form-params}
        (if (verify-key (body "token"))
          (handle-request body)
          (send-response "Who are you even?")))
  (route/not-found "Not Found"))

(defn wrap-log-request [handler]
  "console log all the requests"
  (fn [req]
    (println req)
    (handler req)))

(def app
  (-> app-routes
      wrap-params
      wrap-json-response))

