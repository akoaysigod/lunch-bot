(ns lunch-bot.vote
  (use [clojure.string :only (join)]))

(def votes (atom {}))
(def counter (atom 0))

(defn- parse-vote [user vote]
  (let [place (join " " vote)]
    (cond
      (= place "") "You didn't vote for anything."
      (not (nil? (get votes user))) "You already voted."
      (and (number? place) (not (get votes place))) "That's not a choice."
      :else
      (do
        (swap! counter inc)
        (swap! votes assoc user vote)
        (swap! votes @counter vote)
        "Yay you voted."))))

(defn start [user vote]
  (let [time (quot (System/currentTimeMillis) 1000)]
    (cond
      (or (nil? (get votes :time)) (> (- time (vote :time)) 600))
      (do
        (reset! votes {})
        (reset! counter 0)
        (swap! votes assoc :time time)))
    (parse-vote user vote)))
