(ns hive-help.core
  "Pure helpers for actionable Hive error/help messages."
  (:require [clojure.string :as str]))

(defn type-name
  [value]
  (cond
    (nil? value) "nothing"
    (string? value) "string"
    (integer? value) "integer"
    (number? value) "number"
    (keyword? value) "keyword"
    (map? value) "map"
    (vector? value) "vector"
    (sequential? value) "sequence"
    (boolean? value) "boolean"
    :else (.getSimpleName (class value))))

(defn backtick [x] (str "`" (name x) "`"))

(defn join-lines
  [& lines]
  (->> lines (remove nil?) (str/join "\n")))

(defn bullet-list
  [items]
  (->> items (map #(str "  - " %)) (str/join "\n")))

(defn format-example
  [example]
  (cond
    (nil? example) nil
    (string? example) example
    :else (pr-str example)))

(defn expected-message
  "Build an Elm-style expectation message."
  [{:keys [param expected actual actual-type hint example wrong right]}]
  (join-lines
   (format "I was expecting %s for %s but got %s: %s"
           expected
           (backtick param)
           (or actual-type (type-name actual))
           (pr-str actual))
   ""
   (when hint (str "HINT: " hint))
   (when example
     (join-lines "" "Example:" (str "    " (format-example example))))
   (when (or wrong right)
     (join-lines ""
                 (when wrong (str "WRONG: " wrong))
                 (when right (str "RIGHT: " right))))))

(defn command-message
  "Message for invalid command/tool dispatch."
  [{:keys [tool command valid-commands examples hint]}]
  (join-lines
   (format "Unknown command%s: %s"
           (if tool (str " for " (backtick tool)) "")
           (pr-str command))
   ""
   (if (seq valid-commands)
     (join-lines "Available commands:"
                 (bullet-list (map name valid-commands)))
     "Available commands: none were registered by the caller.")
   (when hint (join-lines "" (str "HINT: " hint)))
   (when (seq examples)
     (join-lines "" "Examples:"
                 (bullet-list (map format-example examples))))))

(defn levenshtein-distance
  "Dependency-free edit distance for command suggestions."
  [a b]
  (let [a (str a)
        b (str b)
        m (count a)
        n (count b)]
    (loop [i 0
           prev (vec (range (inc n)))]
      (if (= i m)
        (nth prev n)
        (let [ca (nth a i)
              curr (loop [j 0
                          row [(inc i)]]
                     (if (= j n)
                       row
                       (let [cost (if (= ca (nth b j)) 0 1)
                             deletion (inc (nth prev (inc j)))
                             insertion (inc (peek row))
                             substitution (+ (nth prev j) cost)]
                         (recur (inc j)
                                (conj row (min deletion insertion substitution))))))]
          (recur (inc i) curr))))))

(defn suggest
  ([input choices] (suggest input choices 3))
  ([input choices limit]
   (->> choices
        (map (fn [choice]
               {:choice choice
                :distance (levenshtein-distance (name input) (name choice))}))
        (sort-by (juxt :distance (comp name :choice)))
        (take limit)
        (mapv :choice))))

(defn unknown-command
  [{:keys [tool command valid-commands examples]}]
  (let [suggestions (suggest command valid-commands 3)]
    (command-message
     {:tool tool
      :command command
      :valid-commands valid-commands
      :hint (when (seq suggestions)
              (str "Did you mean "
                   (str/join ", " (map backtick suggestions))
                   "?"))
      :examples examples})))
