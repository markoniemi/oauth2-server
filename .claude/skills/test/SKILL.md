---
name: test
description: "Use when asked to run the unit tests of oauth2-server (JUnit, no integration tests, no Docker needed) and report a summary."
---

Run unit tests (surefire) for all modules, then report a summary of results.

Run from the repository root. The Maven `test` phase runs surefire only, so integration tests (`*IT`) are not run and no Docker image is built.

```bash
mvn test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Before reporting, check that `auth-server/target/surefire-reports/*.txt` were modified by this run. If they are stale, say so and do not report the tests as passing.

After running, summarize:
- How many tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
