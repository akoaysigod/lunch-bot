(ns lunch-bot.yelp
  (:require [gws.yelp.client :as client]
            [gws.yelp.api :as api]))

(def ^:private yelp-key (System/getenv "YELP_CONSUMER_KEY"))
(def ^:private yelp-consumer-secret (System/getenv "YELP_CONSUMER_SECRET"))
(def ^:private yelp-token (System/getenv "YELP_TOKEN"))
(def ^:private yelp-token-secret (System/getenv "YELP_TOKEN_SECRET"))

(def ^:private yelp-client
  (client/create yelp-key yelp-consumer-secret yelp-token yelp-token-secret))

(def ^:private zipcode "07302")

(defn- parse-single-result [result]
  (result :url))

(defn get-random []
  (let [results (api/search yelp-client
                             {:term "restaurants"
                              :location zipcode
                              :limit 10
                              :sort (rand-int 4)
                              :radius_filter 1000})]
    (if (nil? (results :businesses))
      ("There was a problem. Sorry.")
      (-> (results :businesses)
          rand-nth
          parse-single-result))))

;;TODO: increment + units -> radius
;;TODO: location string or lat lng
(defn get-by-query [category increment units location]
  (let [results (api/search yelp-client
                             {:term category
                              :location location
                              :limit 10
                              :sort (rand-int 3)
                              :radius_filter 1000})]
    (if (nil? (results :businesses))
      (do
        (println "yelp results" results)
        "There was a problem. Sorry.")
      (-> (results :businesses)
          rand-nth
          parse-single-result))))
