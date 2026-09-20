(ns hive-help.namespaced-test
  "A namespaced command vocabulary has to survive being rendered.

   `name` drops a keyword's namespace, so a catalog of :project/info,
   :media/list and :timeline/insert-clip used to list as `info`, `list` and
   `insert-clip`. Worse, `suggest` measured edit distance over those bare
   names, so a mistyped `timeline/insert-clips` came back with
   `start`, `abort`, `opacity`: every route in one namespace looked equally
   close to every route in another.

   Found 2026-09-20 from hive-kdenlive, whose route catalog is 35 qualified
   keywords."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [hive-help.core :as help]))

(def ^:private routes
  [:project/info :project/new :project/open :project/save
   :media/import :media/list
   :timeline/tracks :timeline/add-track :timeline/insert-clip
   :timeline/insert-clips-batch
   :clip/resize :clip/move :clip/volume
   :render/start :render/jobs :render/abort
   :playback/play :playback/position])

;; ---------------------------------------------------------------------------
;; label

(deftest label-keeps-a-namespace-and-changes-nothing-else-test
  (testing "a qualified keyword keeps its namespace"
    (is (= "timeline/insert-clip" (help/label :timeline/insert-clip)))
    (is (= "project/info" (help/label :project/info))))
  (testing "an unqualified keyword is unchanged"
    (is (= "render" (help/label :render))))
  (testing "a string is unchanged"
    (is (= "new_canvas" (help/label "new_canvas"))))
  (testing "a qualified symbol keeps its namespace too"
    (is (= "hive-creator.run/prepare" (help/label 'hive-creator.run/prepare))))
  (testing "anything else still renders"
    (is (= "42" (help/label 42)))))

(deftest backtick-keeps-the-namespace-test
  (is (= "`timeline/insert-clip`" (help/backtick :timeline/insert-clip)))
  (testing "unqualified is unchanged, so existing callers see no difference"
    (is (= "`width`" (help/backtick :width)))
    (is (= "`width`" (help/backtick "width")))))

;; ---------------------------------------------------------------------------
;; the listing

(deftest every-command-is-listed-with-its-namespace-test
  (let [message (help/command-message {:tool "kdenlive_call"
                                       :command "nope"
                                       :valid-commands routes})]
    (doseq [r routes]
      (is (str/includes? message (help/label r))
          (str r " lost its namespace in the listing")))
    (testing "the bare name alone is no longer what is offered"
      (is (not (str/includes? message "\n  - info\n")) message)
      (is (str/includes? message "\n  - project/info\n") message))))

;; ---------------------------------------------------------------------------
;; the suggestions

(deftest a-near-miss-in-the-same-namespace-wins-test
  (testing "the obvious typo is suggested first"
    (is (= :timeline/insert-clip (first (help/suggest :timeline/insert-clips routes)))))
  (testing "the suggestions stay inside the namespace that was asked for"
    (let [top (help/suggest :timeline/insert-clips routes 3)]
      (is (every? #(= "timeline" (namespace %)) top)
          (str "measuring bare names used to scatter these across namespaces: "
               (pr-str top))))))

(deftest suggestions-over-unqualified-choices-are-unchanged-test
  (let [commands ["new_canvas" "place_text" "place_image" "export_image"]]
    (is (= "place_text" (first (help/suggest "place_txt" commands))))
    (is (= "new_canvas" (first (help/suggest "new_cavnas" commands))))))

(deftest unknown-command-carries-the-namespaced-suggestion-test
  (let [message (help/unknown-command {:tool "kdenlive_call"
                                       :command :timeline/insert-clips
                                       :valid-commands routes})]
    (is (str/includes? message "timeline/insert-clip") message)
    (testing "the hint is the part a reader acts on"
      (is (str/includes? message "Did you mean") message))))
