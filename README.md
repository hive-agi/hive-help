# hive-help

Shared Clojure helpers for actionable Hive error and help messages.

The library is intentionally pure: it returns strings and data-first result
maps, leaving MCP/CLI/Emacs callers to choose their own response envelope.

## Scope

- command/tool suggestions
- expected/actual argument messages
- coercion helpers with actionable failures
- hive-test trifecta coverage for golden, property, and mutation checks

## Test

```sh
clojure -M:test
```
