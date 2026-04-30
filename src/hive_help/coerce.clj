(ns hive-help.coerce
  "Coercion helpers returning data-first success/error maps."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [hive-help.core :as help]))

(defn ok [value] {:ok value})

(defn error [message data]
  {:error message :data data})

(defn coerce-int
  ([value param] (coerce-int value param nil))
  ([value param default]
   (cond
     (nil? value)
     (if (some? default)
       (ok default)
       (error (help/expected-message
               {:param param
                :expected "an integer"
                :actual value
                :hint "This parameter is required."
                :example {param 10}})
              {:param param :value value :expected :integer}))

     (integer? value)
     (ok value)

     (string? value)
     (try
       (ok (Long/parseLong value))
       (catch NumberFormatException _
         (error (help/expected-message
                 {:param param
                  :expected "an integer"
                  :actual value
                  :hint "Pass an integer, not a string."
                  :wrong (format "%s: %s" (name param) (pr-str value))
                  :right (format "%s: %s" (name param) (or default 10))})
                {:param param :value value :expected :integer})))

     :else
     (error (help/expected-message
             {:param param
              :expected "an integer"
              :actual value
              :example {param (or default 10)}})
            {:param param :value value :expected :integer}))))

(defn coerce-vec
  ([value param] (coerce-vec value param nil))
  ([value param default]
   (cond
     (nil? value)
     (ok (or default []))

     (vector? value)
     (ok value)

     (sequential? value)
     (ok (vec value))

     (string? value)
     (if (str/starts-with? (str/trim value) "[")
       (try
         (let [parsed (json/read-str value)]
           (if (sequential? parsed)
             (ok (vec parsed))
             (error (help/expected-message
                     {:param param
                      :expected "an array"
                      :actual parsed
                      :hint "Provide a JSON array."
                      :example {param ["item1" "item2"]}})
                    {:param param :value value :expected :vector})))
         (catch Exception e
           (error (help/expected-message
                   {:param param
                    :expected "a valid JSON array"
                    :actual value
                    :hint (.getMessage e)
                    :example {param ["item1" "item2"]}})
                  {:param param :value value :expected :vector})))
       (error (help/expected-message
               {:param param
                :expected "an array"
                :actual value
                :hint "Provide a JSON array, not a plain string."
                :wrong (format "%s: %s" (name param) (pr-str value))
                :right (format "%s: [\"item1\", \"item2\"]" (name param))})
              {:param param :value value :expected :vector}))

     :else
     (error (help/expected-message
             {:param param
              :expected "an array"
              :actual value
              :example {param ["item1" "item2"]}})
            {:param param :value value :expected :vector}))))
