(ns lunch-bot.send
  (require [clj-http.client :as client]
           [clojure.data.json :as json]))

(def ^:private slack-url (System/getenv "SLACKURL"))
(def ^:private yelp-logo "https://s3-media3.fl.yelpassets.com/assets/2/www/img/2d7ab232224f/developers/yelp_logo_100x50.png")

(defn send-response [resp]
  (let [text {:text resp}
        add-user (assoc text :username "lunch-bot") ;;is there a better way to handle this?
        add-hamburger (assoc add-user :icon_emoji ":hamburger:")
        payload {:form-params {:payload (json/write-str add-hamburger)}}]
  (client/post slack-url payload)))

(defn yelp-branding [name url category address stars-url review-count rating]
  (let [branding [{:fallback url
                   :title name
                   :title_link url
                   :text (str category " at " address "\n" rating " based on " review-count " reviews.")
                   :image_url stars-url}]]
    (map #(assoc % :color "af0606") branding)))

(def ^:private yelp-logo
  {:fallback "Powered by Yelp."
   :text "Powered by: "
   :image_url yelp-logo
   :color "af0606"})

(defn send-attachment [attachment]
  (let [add-user {:username "lunch-bot"} ;;is there a better way to handle this?
        add-hamburger (assoc add-user :icon_emoji ":hamburger:")
        add-attachments (assoc add-hamburger :attachments (concat attachment [yelp-logo]))
        payload {:form-params
                 {:payload (json/write-str add-attachments)}}]
  (client/post slack-url payload)))

