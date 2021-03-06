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

;; Sort mode: 0=Best matched (default), 1=Distance, 2=Highest Rated
(def ^:private yelp-sort-options {:best 0 :distance 1 :highest-rated 2})

(def ^:private zipcode "07302")
(def ^:private default-params {:term "restaurants"
                               :location zipcode
                               :limit 3
                               :sort 0
                               :radius_filter 1000})

(defn- yelp-query
  [& {:keys [term location limit sort radius-filter]
      :or {term "restaurant" location zipcode limit 10 sort 0 radius-filter 1000}}]
  (api/search yelp-client {:term term
                           :location location
                           :limit limit
                           :sort sort
                           :radius_filter radius-filter}))

(defn- parse-single [result]
  (let [url (result :url)
        name (result :name)
        rating-img (result :rating_img_url)
        rating (result :rating)
        review-count (result :review_count)
        food-image (result :image_url)
        category (first (first (result :categories)))
        address (clojure.string/join " " (and (result :location) ((result :location) :display_address)))
        phone (result :display_phone)]
    (send/yelp-branding name url category address rating-img review-count rating)))

(defn respond [results description]
  "Takes list of results and slack text object describing the results, sends http response"
  (let [branded-results (map parse-single results)
        flattened-branded-results (flatten branded-results)]
    (send/send-attachment (conj flattened-branded-results description))))

(defn get-random [user command]
  (let [results (yelp-query :sort (rand-int 3))]
    (if (nil? (results :businesses))
      "There was a problem. Sorry."
      (let [random (rand-nth (results :businesses))]
        (respond [random] {:fallback "lunch-bot command"
                        :text (str user " said \"" command "\". results:\n")
                        :color "ffffff"})))))

(defn- convert-to-meters [increment units]
  (let [units (if (nil? units) "kilometers" (clojure.string/lower-case units))
        increment (Integer/parseInt increment)]
    ;;^might be a better way to handle this
    (cond
      (or (nil? increment) (nil? units)) nil
      (or (re-matches #"miles*" units) (= units "mi")) (* 1609 increment)
      (or (re-matches #"meters*" units) (= units "m")) (* 1 increment)
      (or (re-matches #"kilometers*" units) (= units "km")) (* 1000 increment)
      :else nil)))

(defn- sort-text-to-mode
  "Converts English sort criteria to yelp sort mode digit"
  [sort-text]
  (if (nil? sort-text)
   nil
   (let [sort-text (clojure.string/trim sort-text)]
    (cond
      (re-matches #"best" sort-text) (yelp-sort-options :best)
      (re-matches #"closest" sort-text) (yelp-sort-options :distance)
      (re-matches #"highest.rated" sort-text) (yelp-sort-options :highest-rated)
      :else nil))))

(defn- limit-text-to-limit
  "Converts English limit criteria (ex \"top 5\") to integer"
  [limit-text]
  (if (nil? limit-text)
    nil
    (let [limit-text (clojure.string/trim limit-text)
          [adjective limit] (rest (re-matches #"(\w+) (\d+)" limit-text))]
       (Integer/parseInt limit))))

(defn- parse-query [text]
  "Command structure -> [limit-text] [sort-text] [category] within [increment] [units] of [location]"
  (let [[limit-text sort-text term increment units location]
        (rest (re-matches #"(top \d+ )*(best |closest |highest.rated )*(.+) within (\d+) (\w+) of (.+)" text))
        radius (or (convert-to-meters increment units) (default-params :radius_filter))
        limit (or (limit-text-to-limit limit-text) (default-params :limit))
        sort (or (sort-text-to-mode sort-text) (default-params :sort))]
    (if (nil? term) nil
        {:term term
         :limit limit
         :radius_filter radius
         :location location
         :sort sort})))

(defn- get-by-query [params]
  (let [{term :term radius :radius_filter location :location sort :sort limit :limit} params
        results (yelp-query :term term :radius_filter
                            radius :location location :sort sort :limit limit)]
    ;;^this is still pretty ugly
    (if (nil? (results :businesses))
      (str "There was a problem. " (get-in results [:error :text] "Sorry.")))
    (results :businesses)))

(defn handle-query-request
  [user command text]
  (let [parsed (parse-query text)]
    (if (nil? parsed) (str "Unable to parse " text)
    (let [results (get-by-query parsed)]
      ; add user + command info to the tail of the result set before sending
      (respond results {:fallback "lunch-bot command"
                     :text (str user " said \"" command " " text "\". results:\n")
                     :color "ffffff"})))))
