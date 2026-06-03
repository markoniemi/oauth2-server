---
name: test-all
description: "Run frontend unit tests, backend unit tests, and backend integration tests sequentially, then report a summary of all results."
---

# Test All (Unit + Integration)

Run full test suite: frontend unit tests, backend unit tests, backend integration tests.

## How to use

1. Invoke `/test-all` in the conversation
2. Wait for all three phases to complete
3. Review summary report with pass/fail counts

## Test phases

**Phase 2: Backend unit tests**
- Command: `mvn test -DskipITs -q`
- Framework: JUnit 5 + Mockito
- Excludes: Integration tests

**Phase 3: Backend integration tests**
- Command: `mvn test -q`
- Framework: Spring Boot Test + Testcontainers
- Includes: Database, container tests

## Reporting

Summary shows:
- Backend unit: tests passed/failed
- Backend integration: tests passed/failed
- All failing tests listed with error messages
- Overall status: ✓ all pass or ✗ failures

## When to use

- Before pushing to remote
- Verifying full functionality
- CI/CD validation
- Regression testing
