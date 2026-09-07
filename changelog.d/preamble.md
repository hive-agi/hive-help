# Changelog

Notable changes to hive-help, derived from the conventional-commit history
between release tags by `clojure -T:build changelog` (hive-build). Format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning
follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This file is GENERATED on every release and rewritten in full. Prose for a
release belongs in `changelog.d/<version>.md`, which is spliced in under that
release's heading and never overwritten. This header is `changelog.d/preamble.md`.

## What the version number promises

hive-help is a leaf: two dependencies, pure functions, strings and maps out.
A public name in `hive-help.core`, `hive-help.coerce` or `hive-help.diag`
does not disappear or narrow without a MAJOR bump; the release workflow's
`freeze-check` refuses the release otherwise. Adding a function, an arity or
an optional key is a minor or patch change. The exact wording of a message is
not part of the promise: callers match on the result maps, not on the prose.
