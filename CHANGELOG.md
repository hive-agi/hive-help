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

## [0.1.3] - 2026-09-06

### Added

- **release:** publish to Clojars again; the repo is public, so the target follows
- **release:** private-registry deploy target + CI

### Fixed

- **kondo:** scan Babashka dependencies [skip ci]
- **build:** idempotent deploy — skip if version already on Clojars
- **build:** read VERSION file (align Clojars coord with existing git tags); restore release.yml, Clojars in separate clojars.yml

_Plus 17 routine commits (docs, tests, build, chores)._

## [0.1.0] - 2026-04-30

_No user-facing changes._

_Plus 1 routine commit (docs, tests, build, chores)._
