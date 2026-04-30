(ns hive-help.core-trifecta-test
  (:require [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [hive-help.coerce :as coerce]
            [hive-help.core :as help]
            [hive-test.trifecta :refer [deftrifecta]]))

(defn run-unknown-command [case]
  (help/unknown-command case))

(defn run-coerce-int [{:keys [value param default]}]
  (coerce/coerce-int value param default))

(defn run-coerce-vec [{:keys [value param default]}]
  (coerce/coerce-vec value param default))

(defn actionable-message? [s]
  (and (string? s)
       (str/includes? s "Unknown command")
       (str/includes? s "Available commands")
       (str/includes? s "HINT:")
       (str/includes? s "Examples:")))

(defn coerce-result? [result]
  (and (map? result)
       (or (contains? result :ok)
           (and (string? (:error result))
                (map? (:data result))))))

(def ^:private gen-command-case
  (gen/let [command gen/string-alphanumeric]
    {:tool :kg
     :command command
     :valid-commands [:traverse :edge :stats]
     :examples [{:tool "kg" :command "traverse" :start_node "id"}]}))

(def ^:private gen-int-case
  (gen/let [value (gen/one-of [gen/int
                               gen/string-alphanumeric
                               (gen/return nil)])
            default (gen/one-of [gen/int (gen/return nil)])]
    {:value value :param :limit :default default}))

(def ^:private gen-vec-case
  (gen/let [value (gen/one-of [(gen/vector gen/string-alphanumeric)
                               (gen/list gen/string-alphanumeric)
                               (gen/return "[\"a\",\"b\"]")
                               gen/string-alphanumeric
                               (gen/return nil)])]
    {:value value :param :tags :default []}))

(deftrifecta unknown-command-actionability
  hive-help.core-trifecta-test/run-unknown-command
  {:golden-path "test/golden/hive-help/unknown-command.edn"
   :cases {:kg-typo {:tool :kg
                     :command "travrese"
                     :valid-commands [:traverse :edge :stats]
                     :examples [{:tool "kg" :command "traverse" :start_node "id"}]}
           :memory-typo {:tool :memory
                         :command "qurey"
                         :valid-commands [:query :add :get]
                         :examples [{:tool "memory" :command "query" :query "datahike"}]}}
   :gen gen-command-case
   :pred actionable-message?
   :num-tests 50
   :mutations [["no-hint" (fn [{:keys [tool command valid-commands examples]}]
                            (help/command-message
                             {:tool tool
                              :command command
                              :valid-commands valid-commands
                              :examples examples}))]]})

(deftrifecta coerce-int-contract
  hive-help.core-trifecta-test/run-coerce-int
  {:golden-path "test/golden/hive-help/coerce-int.edn"
   :cases {:already-int {:value 42 :param :limit :default 10}
           :string-int {:value "42" :param :limit :default 10}
           :bad-string {:value "abc" :param :limit :default 20}
           :nil-default {:value nil :param :limit :default 5}}
   :gen gen-int-case
   :pred coerce-result?
   :num-tests 50
   :mutations [["always-ok" (fn [_] {:ok 0})]]})

(deftrifecta coerce-vec-contract
  hive-help.core-trifecta-test/run-coerce-vec
  {:golden-path "test/golden/hive-help/coerce-vec.edn"
   :cases {:already-vec {:value ["a" "b"] :param :tags :default []}
           :seq-value {:value '("a" "b") :param :tags :default []}
           :json-array {:value "[\"a\",\"b\"]" :param :tags :default []}
           :plain-string {:value "a,b" :param :tags :default []}}
   :gen gen-vec-case
   :pred coerce-result?
   :num-tests 50
   :mutations [["always-empty" (fn [_] {:ok []})]]})
