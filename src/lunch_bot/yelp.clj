(ns lunch-bot.yelp
  (:require [gws.yelp.client :as client]
            [gws.yelp.api :as api]))

(def yelp-key (System/getenv "YELP_CONSUMER_KEY"))
(def yelp-consumer-secret (System/getenv "YELP_CONSUMER_SECRET"))
(def yelp-token (System/getenv "YELP_TOKEN"))
(def yelp-token-secret (System/getenv "YELP_TOKEN_SECRET"))

(def yelp-client (client/create yelp-key yelp-consumer-secret yelp-token yelp-token-secret))

(def zipcode "07302")

(defn parse-single-result [result]
  (result :url))

(defn get-random []
  (parse-single-result
   (rand-nth
    ((api/search yelp-client
                 {:term "restaurants"
                  :location zipcode
                  :limit 10
                  :sort (rand-int 4)
                  :radius_filter 1000}) :businesses))))
