(ns lunch-bot.yelp
  (:require [gws.yelp.client :as client]
            [gws.yelp.api :as api]
            [lunch-bot.send :as send]))

(def ^:private yelp-key (System/getenv "YELP_CONSUMER_KEY"))
(def ^:private yelp-consumer-secret (System/getenv "YELP_CONSUMER_SECRET"))
(def ^:private yelp-token (System/getenv "YELP_TOKEN"))
(def ^:private yelp-token-secret (System/getenv "YELP_TOKEN_SECRET"))

(def ^:private yelp-client
  (client/create yelp-key yelp-consumer-secret yelp-token yelp-token-secret))

(def ^:private zipcode "07302")

(defn- parse-single-result [result]
  (->
    (str (result :name) " - " (first (first (result :categories))) " - " (result :url))
    (send/send-response))
    "done parse-single-result" result)

(defn get-random []
  (let [results (api/search yelp-client
                             {:term "restaurants"
                              :location zipcode
                              :limit 10
                              :sort (rand-int 3)
                              :radius_filter 1000})]
    (if (nil? (results :businesses))
      "There was a problem. Sorry."
      (-> (results :businesses)
          rand-nth
          parse-single-result))))

;;TODO: location string or lat lng
(defn- get-by-query [term radius location]
  (let [results (api/search yelp-client
                             {:term term
                              :location location
                              :limit 10
                              :sort (rand-int 3)
                              :radius_filter radius})]
    (if (nil? (results :businesses))
      (do
        (println "yelp results" results)
        (str "There was a problem: " (get-in results [:error :text] "Sorry."))
      (-> (results :businesses)
          rand-nth
          parse-single-result)))))

(defn- convert-to-meters [increment units]
  (let [units (clojure.string/lower-case units)
        increment (Integer/parseInt increment)]
    (cond
      (or (re-matches #"miles*" units) (= units "mi")) (* 1609 increment)
      (or (re-matches #"meters*" units) (= units "m")) (* 1 increment)
      (or (re-matches #"kilometers*" units) (= units "km")) (* 1000 increment)
      :else nil)))

(defn handle-query-request 
  ;; command structure -> [category] within [increment] [unit] of [location]
  [text]
  (let [[term increment units location] (rest (re-matches #"(\w+) within (\d+) (\w+) of (.+)" text))
        radius (convert-to-meters increment units)]
    (if (nil? term)
      (str "Unparsable request " text)
      (get-by-query term radius location))))
