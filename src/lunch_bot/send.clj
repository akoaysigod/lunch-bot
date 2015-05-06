(ns lunch-bot.send
  (require [clj-http.client :as client]
           [clojure.data.json :as json]))

(def ^:private slack-url (System/getenv "SLACKURL"))

(defn send-response [resp]
  (let [text {:text resp}
        add-user (assoc text :username "lunch-bot") ;;is there a better way to handle this?
        add-hamburger (assoc add-user :icon_emoji ":hamburger:")
        payload {:form-params {:payload (json/write-str add-hamburger)}}]
  (client/post slack-url payload)))

