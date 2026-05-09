# Claude Development Guidelines

This document provides development guidelines for this OAuth2 Authorization Server project with integrated TestContainers support.

## Quick Reference

- **Start here**: [README.md](../README.md) — Project overview, quick start, and architecture
- **Testing guide**: [OAuth2TestContainersUsage.md](../OAuth2TestContainersUsage.md) — Complete API reference for TestContainers integration
- **Tech details**: [docs/TechSpec.md](../docs/TechSpec.md) — Auth server architecture and configuration
- **Code standards**: [.github/copilot-instructions.md](../.github/copilot-instructions.md) — Coding conventions and development standards

## Architecture Overview

### OAuth2 Authorization Server
A Spring Boot 3.5.6 OAuth2 Authorization Server with:
- **OAuth2 Authorization Code flow** with PKCE support
- **OpenID Connect** discovery endpoint
- **JWT token** generation
- **Form login** authentication
- **Custom user/role** management via YAML or fluent API

### TestContainers Integration
Reusable library for testing downstream applications:
- Spin up auth server in Docker during tests
- Fluent builder API for user and client configuration
- YAML file support for complex setups
- Custom issuer URL and context path support
- Full lifecycle management

### Key Components

**Container** — Main entry point. Extends `GenericContainer<Container>` to manage the Docker container lifecycle and configuration.

**Client & User** — Data classes (Java records) representing registered clients and authentication users with validation.

**ServerConfig** — Configuration aggregator supporting both fluent builder and YAML file-based setup.

**ContainerRegisteredClientConfig** — Spring configuration class that registers clients into the authorization server's repository.

For detailed information about coding conventions and development practices, refer to the [Copilot Instructions](../.github/copilot-instructions.md).

## Git Commit Messages

Commit messages use a **one-line format with semicolons** to separate concerns:

```
Brief action; additional change; optional note
```

**Examples:**
- `Add TestContainers library; support fluent builder and YAML config; enable testing with containerized auth server`
- `Simplify user configuration: use records instead of classes; add Lombok @Data where safe`
- `Refactor Client: manual constructor validation instead of Lombok @Builder to prevent null bypasses`

**Guidelines:**
- One line only — concise and scannable in git log
- Use semicolons to separate multiple logical changes
- Use imperative mood: "add", "fix", "refactor" (not "added", "fixed")
- Focus on **what changed and why**, not implementation details
- Capitalize first word
- No period at end

## Implementation Tasks

During implementation tasks (planning, coding, testing):
- **Do not commit** unless explicitly asked
- Work iteratively and validate completeness before committing
- Use feature branches for significant work
- Plan all changes upfront before execution
