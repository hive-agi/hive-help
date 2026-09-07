# hive-help

Shared Clojure helpers for actionable Hive error and help messages.

The library is intentionally pure: it returns strings and data-first result
maps, leaving MCP/CLI/Emacs callers to choose their own response envelope.

## Scope

- command/tool suggestions
- expected/actual argument messages
- coercion helpers with actionable failures
- hive-test trifecta coverage for golden, property, and mutation checks

## Usage

Every `clojure` block below runs in a new JVM on every release
(`clojure -T:build readme-examples`), and every `;; =>` line is a claim the
run refutes when it stops being true. The values shown are observed, not
paraphrased.

```clojure
(require '[hive-help.core :as help]
         '[hive-help.coerce :as coerce]
         '[hive-help.diag :as diag])
```

Suggest what the caller probably meant, closest first, ties broken by name:

```clojure
(help/suggest "travrese" [:traverse :edge :stats])
;; => [:traverse :stats :edge]

(help/suggest "qurey" [:query :search :get :add] 2)
;; => [:query :get]

(help/levenshtein-distance "kitten" "sitting")
;; => 3
```

Name a value's type the way a message should:

```clojure
(help/type-name [1 2])
;; => "vector"

(help/type-name nil)
;; => "nothing"
```

Coerce MCP-style string arguments, answering `{:ok v}` or `{:error msg :data d}`:

```clojure
(coerce/coerce-int "10" :limit)
;; => {:ok 10}

(coerce/coerce-int nil :limit 25)
;; => {:ok 25}

(:data (coerce/coerce-int "ten" :limit))
;; => {:param :limit, :value "ten", :expected :integer}

(coerce/coerce-vec "[\"a\",\"b\"]" :tags)
;; => {:ok ["a" "b"]}

(coerce/coerce-vec nil :tags)
;; => {:ok []}
```

The messages are Elm-shaped: what was expected, what arrived, a hint, and
the right spelling.

```clojure
(println (:error (coerce/coerce-int "ten" :limit)))
```

```text
I was expecting an integer for `limit` but got string: "ten"

HINT: Pass an integer, not a string.

WRONG: limit: "ten"
RIGHT: limit: 10
```

An unknown command answers with the valid ones and the nearest spellings:

```clojure
(println (help/unknown-command {:tool :kg
                                :command "travrese"
                                :valid-commands [:traverse :edge :stats]
                                :examples [{:tool "kg" :command "traverse" :start_node "id"}]}))
```

```text
Unknown command for `kg`: "travrese"

Available commands:
  - traverse
  - edge
  - stats

HINT: Did you mean `traverse`, `stats`, `edge`?

Examples:
  - {:tool "kg", :command "traverse", :start_node "id"}
```

`hive-help.diag` builds project-level diagnostics in the same shape:
missing config files, unresolvable scopes, unreachable paths, timeouts.

```clojure
(println (diag/timeout-message {:operation  "sidecar analysis"
                                :target     "hive-mcp"
                                :timeout-ms 120000
                                :hint       "A cold classpath resolve can take longer than the analysis."
                                :command    "clojure -Spath"}))
```

```text
sidecar analysis timed out after 120s on "hive-mcp".

HINT: A cold classpath resolve can take longer than the analysis.

Diagnostic command:
  clojure -Spath
```

## Test

```sh
clojure -M:test
```

## Release

Pushing `main` with a change under `src/` releases. Three gates run before
the version is bumped: the suite, `freeze-check` (the public surface stays
additive between releases, per `freeze-policy.edn`) and `readme-examples`
(this file's claims). `changelog` then regenerates `CHANGELOG.md` from the
commits, with `changelog.d/preamble.md` as its header, and the version is
tagged and published to Clojars. Nothing in `CHANGELOG.md` is edited by hand.
