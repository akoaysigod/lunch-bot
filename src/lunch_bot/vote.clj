(ns lunch-bot.vote
  (use [clojure.string :only (join)])
  (require [lunch-bot.send :as send]))

(def votes (atom {:counts {}}))
(def display (atom {}))
(def counter (atom 1))

(defn- send-response [v counts]
  (let [arr (seq counts)]
    (-> 
     (reduce #(str %1 "(" (first %2) ") " (v (first %2)) " - " (second %2) "\n") "" arr)
     (send/send-response))))

(defn- increase-count [c v]
  (let [n ((v :counts) c)
        counts (v :counts)
        exists? (if (nil? n) false true)
        count (if exists? (inc n) 1)
        new-counts (assoc counts c count)]
    (swap! votes assoc :counts new-counts)))

(defn- parse-vote [user vote]
  (let [place (join " " vote)
        number (read-string place)
        not-choice? (and (number? number) (nil? (@votes number)))
        choice? (and (number? number) (some? (@votes number)))
        choice (if choice? number @counter)]
    (cond
      (= place "") "You didn't vote for anything."
      (not (nil? (@votes user))) "You already voted."
      not-choice? "That's not a choice."
      :else
      (do
        (if (not choice?) (swap! counter inc))
        (if (not choice?) (swap! votes assoc choice vote))
        (swap! votes assoc user (if choice? (@votes choice) vote))
        (increase-count choice @votes)
        (send-response @votes (@votes :counts))
        "Yay, you voted."))))

(defn start [user vote]
  (let [time (quot (System/currentTimeMillis) 1000)]
    (cond
      (or (nil? (get votes :time)) (> (- time (vote :time)) 600))
      (do
        (reset! votes {:counts {}})
        (reset! counter 0)
        (swap! votes assoc :time time)))
    (parse-vote user vote)))
