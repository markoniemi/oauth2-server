# Claude Development Guidelines

This document provides development guidelines for this project. For detailed Copilot and code generation instructions, please refer to the GitHub Copilot Instructions.

## Quick Reference

- **Copilot Instructions**: See [.github/copilot-instructions.md](../.github/copilot-instructions.md) for comprehensive guidance on coding conventions, architecture patterns, and development standards.

## Key Resources

- **TechnicalSpecification.md**: Defines the technical architecture and stack
- **RequirementsSpecification.md**: Outlines project requirements and features
- **copilot-instructions.md**: Complete coding standards and guidelines for all team members

## Architecture Overview

This is a monolithic Spring Boot application with:
- **Backend**: Java/Spring Boot REST API
- **Frontend**: React/TypeScript SPA packaged as a WebJar
- **Database**: PostgreSQL with H2 for tests
- **Authentication**: OAuth 2.0 with Spring Security
- **Error Handling**: RFC 7807 Problem Details with validation error extensions

### Key Architectural Patterns

**Error Responses** — All API errors follow RFC 7807 (`ProblemDetail`). Validation errors include an `errors` array with field-level details (`{ field, message, code }`). Frontend wires these directly into form field errors using react-hook-form.

**Validation** — Spring Validation Framework (`@Valid`, `@NotBlank`, etc.) on DTOs. Global exception handler extracts field errors and returns them in the `errors` extension of `ProblemDetail`.

For detailed information about coding conventions, architecture patterns, and development practices, refer to the [Copilot Instructions](../.github/copilot-instructions.md).

## Git Commit Messages

Commit messages use a **one-line format with semicolons** to separate concerns:

```
Brief action; additional change; optional note
```

**Examples:**
- `Add i18n for validation messages and form labels; replace 26 hardcoded strings in EditForm and FieldEditor with translation keys`
- `Simplify service contracts: throw on non-JSON instead of returning null; remove nullable returns; simplify empty checks in components`
- `Replace fireEvent with userEvent in 3 test files; modernize test patterns for better UX simulation`

**Guidelines:**
- One line only — concise and scannable in git log
- Use semicolons to separate multiple logical changes
- Use imperative mood: "add", "fix", "refactor" (not "added", "fixed")
- Focus on **what changed and why**, not implementation details
- Capitalize first word
- No period at end
