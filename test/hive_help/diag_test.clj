(ns hive-help.diag-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [hive-help.diag :as diag]))

;; -----------------------------------------------------------------------------
;; unreachable-paths-message

(deftest unreachable-paths-renders-required-fields
  (testing "core ELM elements present"
    (let [msg (diag/unreachable-paths-message
               {:context     "deps.edn at /home/me/proj"
                :consumer    "the sidecar container"
                :unreachable [{:label 'io.foo/bar :path "../bar"}
                              {:label 'io.foo/qux :path "/host/qux"}]})]
      (is (str/includes? msg "I found 2 paths"))
      (is (str/includes? msg "deps.edn at /home/me/proj"))
      (is (str/includes? msg "the sidecar container cannot reach"))
      (is (str/includes? msg "io.foo/bar"))
      (is (str/includes? msg "../bar"))
      (is (str/includes? msg "io.foo/qux"))
      (is (str/includes? msg "/host/qux")))))

(deftest unreachable-paths-singular-grammar
  (let [msg (diag/unreachable-paths-message
             {:context     "deps.edn"
              :consumer    "the sandbox"
              :unreachable [{:label 'a/b :path "/x"}]})]
    (is (str/includes? msg "I found 1 path "))))

(deftest unreachable-paths-optional-fields
  (testing "hint, wrong/right, command, reachable-from all flow through"
    (let [msg (diag/unreachable-paths-message
               {:context        "deps.edn"
                :consumer       "the sidecar"
                :reachable-from "/workspace"
                :unreachable    [{:label 'a/b :path "../b"}]
                :hint           "Use a reachable path."
                :wrong          ":local/root \"../b\""
                :right          [":git/sha \"abc\""
                                 ":local/root \"/workspace/b\""]
                :command        "docker compose restart sidecar"})]
      (is (str/includes? msg "/workspace"))
      (is (str/includes? msg "HINT: Use a reachable path."))
      (is (str/includes? msg "WRONG: :local/root \"../b\""))
      (is (str/includes? msg "RIGHT:"))
      (is (str/includes? msg ":git/sha"))
      (is (str/includes? msg "docker compose restart sidecar")))))

;; -----------------------------------------------------------------------------
;; unresolvable-scope-message

(deftest unresolvable-scope-renders-attempts
  (let [msg (diag/unresolvable-scope-message
             {:scope    "/path/to/project"
              :tried    [{:strategy "project tree" :result "not registered"}
                         {:strategy "absolute path" :result "directory not found"}
                         {:strategy "HIVE_ROOT/<id>" :result "no such directory"}]
              :examples ["hive-mcp"
                         "/home/leibniz/PP/hive/hive-mcp"]
              :hint     "Pass a known project-id or absolute path."})]
    (is (str/includes? msg "I could not resolve"))
    (is (str/includes? msg "/path/to/project"))
    (is (str/includes? msg "Resolution attempts:"))
    (is (str/includes? msg "project tree"))
    (is (str/includes? msg "HIVE_ROOT/<id>"))
    (is (str/includes? msg "Valid examples:"))
    (is (str/includes? msg "hive-mcp"))
    (is (str/includes? msg "HINT:"))))

(deftest unresolvable-scope-minimal
  (testing "only :scope is required"
    (let [msg (diag/unresolvable-scope-message {:scope "x"})]
      (is (str/includes? msg "I could not resolve"))
      (is (str/includes? msg "\"x\""))
      (is (not (str/includes? msg "Resolution attempts:")))
      (is (not (str/includes? msg "Valid examples:"))))))

;; -----------------------------------------------------------------------------
;; missing-config-message

(deftest missing-config-renders
  (let [msg (diag/missing-config-message
             {:context        "the cartography sidecar"
              :expected-file  "/p/deps.edn"
              :searched       ["/p/deps.edn" "/p/project.clj"]
              :create-command "echo '{:deps {}}' > deps.edn"})]
    (is (str/includes? msg "the cartography sidecar requires"))
    (is (str/includes? msg "\"/p/deps.edn\""))
    (is (str/includes? msg "Searched:"))
    (is (str/includes? msg "/p/project.clj"))
    (is (str/includes? msg "echo '{:deps {}}' > deps.edn"))))

;; -----------------------------------------------------------------------------
;; timeout-message

(deftest timeout-renders
  (let [msg (diag/timeout-message
             {:operation  "sidecar analysis"
              :target     "hive-mcp"
              :timeout-ms 60000
              :hint       "Check sidecar logs."
              :command    "docker logs hive-mcp-lsp-sidecar"})]
    (is (str/includes? msg "sidecar analysis timed out after 60s"))
    (is (str/includes? msg "\"hive-mcp\""))
    (is (str/includes? msg "HINT: Check sidecar logs."))
    (is (str/includes? msg "docker logs hive-mcp-lsp-sidecar"))))

(deftest timeout-without-target
  (let [msg (diag/timeout-message
             {:operation  "scan"
              :timeout-ms 5000})]
    (is (str/includes? msg "scan timed out after 5s."))
    (is (not (str/includes? msg " on ")))))
