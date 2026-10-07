---
name: test-all
description: "Use when asked to run all oauth2-server tests — unit tests and integration tests of both auth-server and auth-server-testcontainers — and report a summary."
---

Run unit tests (surefire) and integration tests (failsafe; Docker must be running) for all modules, then report a summary of results.

Run from the repository root, never one module alone: `auth-server` builds the local `ghcr.io/markoniemi/oauth2-server:latest` image (Jib `dockerBuild`, `pre-integration-test`) that the `auth-server-testcontainers` ITs start.

```bash
mvn verify 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Before reporting, check that these were modified by this run:
- `auth-server/target/surefire-reports/*.txt`
- `auth-server/target/failsafe-reports/*.txt`
- `auth-server-testcontainers/target/failsafe-reports/*.txt`

If any are stale, say so and do not report that module as passing.

After running, summarize:
- How many unit and integration tests passed / failed / skipped, per module
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
