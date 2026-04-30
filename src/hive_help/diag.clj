(ns hive-help.diag
  "Project-level ELM-style diagnostic message builders.

   Where `hive-help.core/expected-message` handles param-level
   validation (\"I was expecting `tags` to be an array, but got a
   String...\"), this namespace handles project-level diagnostics:
   unreachable paths, unresolvable scopes, missing config files,
   external-process timeouts. Pure — no IO. Caller passes domain
   data; diag builds the ELM-shaped string.

   See memory `20260124235123-601b98d6` (ELM principle)."
  (:require [clojure.string :as str]
            [hive-help.core :as core]))

(defn- bullet-pair
  "Render `{:label x :path y}` as a two-column bullet line. `path` is
   shown verbatim; `label` is stringified through pr-str so symbols,
   keywords and strings all render unambiguously."
  [{:keys [label path]}]
  (str "  - " (pr-str label) " → " (str path)))

(defn unreachable-paths-message
  "Build an ELM message for paths declared in some config that a
   downstream consumer cannot reach (e.g. host paths invisible to a
   sandboxed container).

   Args (all keys optional unless marked):
     :context         REQUIRED — short label naming the source of the
                      paths (e.g. \"deps.edn at /home/me/proj\").
     :consumer        REQUIRED — short label for what cannot see the
                      paths (e.g. \"the sidecar container\").
     :reachable-from  string describing what the consumer CAN see
                      (e.g. \"/workspace, mounted from /home/me/PP\").
     :unreachable     REQUIRED — vec of {:label any :path str}.
     :hint            extra hint line.
     :wrong           one-line wrong example.
     :right           seq of right-example lines (rendered indented).
     :command         shell command that fixes the situation."
  [{:keys [context consumer reachable-from unreachable hint wrong right command]}]
  (core/join-lines
   (format "I found %d %s declared in %s that %s cannot reach:"
           (count unreachable)
           (if (= 1 (count unreachable)) "path" "paths")
           context
           consumer)
   ""
   (str/join "\n" (map bullet-pair unreachable))
   (when reachable-from
     (core/join-lines "" (str consumer " can only see " reachable-from ".")))
   (when hint (core/join-lines "" (str "HINT: " hint)))
   (when (or wrong (seq right))
     (core/join-lines
      ""
      (when wrong (str "WRONG: " wrong))
      (when (seq right)
        (core/join-lines "RIGHT:" (str/join "\n" (map #(str "       " %) right))))))
   (when command
     (core/join-lines "" "Fix command:" (str "  " command)))))

(defn unresolvable-scope-message
  "Build an ELM message for a scope/identifier that cannot be resolved
   to a concrete target (directory, project, etc).

   Args:
     :scope     REQUIRED — the offending value.
     :tried     vec of {:strategy str :result str} entries describing
                resolution attempts that failed.
     :examples  vec of valid scope examples (strings).
     :hint      extra hint line."
  [{:keys [scope tried examples hint]}]
  (core/join-lines
   (format "I could not resolve %s to a concrete target."
           (pr-str scope))
   (when (seq tried)
     (core/join-lines
      ""
      "Resolution attempts:"
      (str/join "\n"
                (map (fn [{:keys [strategy result]}]
                       (format "  - %s → %s" strategy result))
                     tried))))
   (when hint (core/join-lines "" (str "HINT: " hint)))
   (when (seq examples)
     (core/join-lines
      ""
      "Valid examples:"
      (str/join "\n" (map #(str "  - " %) examples))))))

(defn missing-config-message
  "Build an ELM message for a config file that is required but missing
   at the expected location.

   Args:
     :context         REQUIRED — what needs the config (e.g. \"the
                      cartography sidecar\").
     :expected-file   REQUIRED — file path that was expected.
     :searched        seq of paths that were searched (for diagnosis).
     :create-command  shell command that creates a minimal valid file."
  [{:keys [context expected-file searched create-command]}]
  (core/join-lines
   (format "%s requires %s, but the file does not exist."
           (or context "This operation")
           (pr-str expected-file))
   (when (seq searched)
     (core/join-lines
      ""
      "Searched:"
      (str/join "\n" (map #(str "  - " %) searched))))
   (when create-command
     (core/join-lines
      ""
      "HINT: Create one with:"
      (str "  " create-command)))))

(defn timeout-message
  "Build an ELM message for an external operation that timed out.

   Args:
     :operation   REQUIRED — short label for what timed out (e.g.
                  \"sidecar analysis\").
     :target      what the operation was processing (e.g. project-id).
     :timeout-ms  REQUIRED — the timeout value in ms.
     :hint        extra hint line.
     :command     shell command that diagnoses or unblocks."
  [{:keys [operation target timeout-ms hint command]}]
  (core/join-lines
   (format "%s timed out after %ds%s."
           (or operation "Operation")
           (long (/ timeout-ms 1000))
           (if target (str " on " (pr-str target)) ""))
   (when hint (core/join-lines "" (str "HINT: " hint)))
   (when command
     (core/join-lines "" "Diagnostic command:" (str "  " command)))))
