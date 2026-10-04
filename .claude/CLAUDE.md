# Claude Development Guidelines

This document provides development guidelines for this OAuth2 Authorization Server project with integrated TestContainers support.

## Quick Reference

- **Start here**: [README.md](../README.md) — Project overview, quick start, and architecture
- **Testing guide**: [OAuth2TestContainersUsage.md](../docs/OAuth2TestContainersUsage.md) — Complete API reference for TestContainers integration
- **Tech details**: [docs/TechSpec.md](../docs/TechSpec.md) — Auth server architecture and configuration
- **Code standards**: [.github/copilot-instructions.md](../.github/copilot-instructions.md) — Coding conventions and development standards

## Integration with Dynamic-Form Project

This OAuth2 Authorization Server is used as the authentication provider for the [dynamic-form](../../../dynamic-form) project. **Any changes to this project must be tested with the dynamic-form application** to ensure compatibility, especially when modifying:
- Token generation and JWT claims
- OIDC discovery endpoints
- Client authentication flows
- User/role management

Always validate changes in the dynamic-form test suite or local environment before committing.

## IntelliJ MCP Tool Preferences

When working with this project, prefer IntelliJ IDE MCP tools (`mcp__idea__*`) over generic alternatives:

- **File operations**: Use `mcp__idea__read_file`, `mcp__idea__create_new_file` instead of Bash/generic Read/Write
- **Project navigation**: Use `mcp__idea__search_symbol`, `mcp__idea__search_text`, `mcp__idea__list_directory_tree` for code exploration
- **Building/testing**: Use `mcp__idea__build_project`, `mcp__idea__execute_run_configuration` instead of manual npm/gradle commands
- **Refactoring**: Use `mcp__idea__rename_refactoring` for safe renames across the project
- **Git operations**: Use `mcp__idea__git_status` to check repo state
- **Linting/diagnostics**: Use `mcp__idea__get_file_problems`, `mcp__idea__lint_files` for code quality checks
- **Debugging**: Use `mcp__idea__xdebug_*` tools for stepping through code when needed

IntelliJ MCP tools are aware of the project structure, dependencies, and IDE state, making them more reliable than shell-based alternatives.

## Architecture Overview

### OAuth2 Authorization Server
A Spring Boot 4.0.3 (Spring Security 7) OAuth2 Authorization Server with:
- **OAuth2 Authorization Code flow** with PKCE support
- **OpenID Connect** discovery endpoint
- **JWT token** generation
- **Form login** authentication
- **Custom user/role** management via YAML

### Modules
`auth-server` (Spring Boot app + Docker image) and `auth-server-testcontainers` (library, artifact `com.example:auth-server-testcontainers`). The library never depends on server code; they share only Spring property names.

### TestContainers Integration
Reusable library for testing downstream applications:
- Spin up auth server in Docker during tests
- Bundled server defaults, overridden by a YAML config file
- Custom issuer URL and context path support
- Full lifecycle management

### Key Components

**OAuth2Container** — Main entry point. Extends `GenericContainer<OAuth2Container>` to manage the Docker container lifecycle and configuration.

**Config file** — Users and clients are configured only through `withConfigFile`, which mounts a Spring Boot YAML as the server's `config/application.yaml`. It overrides the server's bundled `application.yaml` defaults (maps merge by key, single values and lists are replaced).

**Server configuration** — Spring Boot auto-configuration registers clients from `spring.security.oauth2.authorizationserver.client.*`; `SecurityConfig` adds filter chains, users (`app.security.users`) and CORS (`app.cors.allowed-origins`).

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
