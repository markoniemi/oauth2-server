Run frontend unit tests (Vitest) and backend unit tests (JUnit, no integration tests), then report a summary of results.

```bash
cd frontend && npx vitest run 2>&1; cd ../backend && mvn test -DskipITs -q 2>&1
```

After running, summarize:
- How many frontend tests passed / failed / skipped
- How many backend tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
