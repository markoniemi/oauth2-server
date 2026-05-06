---
name: dynamic-form-review
description: "Review Java or TypeScript/React code in this project against the project's coding standards (Google Java Style, Spring patterns, React 19 + TypeScript conventions). Invoke with a file path or paste code to review."
---

# Dynamic Form Code Review

Review the specified file (or the file most recently discussed) against this project's standards. If no file is specified, ask the user which file to review.

## How to review

1. Read the target file(s) with the Read tool.
2. Check every item in the relevant checklist below.
3. Report findings grouped by severity:
   - **Must fix** — violates a hard rule (security, correctness, or an explicit project convention)
   - **Should fix** — style or maintainability issue that the project conventions require
   - **Consider** — optional improvement, clearly marked as non-mandatory
4. For each finding include: file path + line number, the rule violated, a one-line explanation, and a concrete fix.
5. After findings, give a short summary paragraph.

Do NOT rewrite the whole file unless the user asks. Propose targeted edits only.

---

## Java / Spring Boot checklist

### Style & formatting
- [ ] Two-space indentation (Google Java Style)
- [ ] Class, method, and variable names are clear and intention-revealing
- [ ] No commented-out code left behind
- [ ] Javadoc used sparingly — only for public API surface or genuinely complex logic

### Architecture & patterns
- [ ] Controllers only delegate — no business logic inside `@RestController` methods
- [ ] Services contain business logic; annotated with `@Service`
- [ ] Entities never returned directly from controllers — DTOs used for all API input/output
- [ ] MapStruct mapper interface present for every entity↔DTO conversion
- [ ] Constructor injection used everywhere (Lombok `@RequiredArgsConstructor`, not `@Autowired`)
- [ ] `@Value` or `@ConfigurationProperties` used for config; no hardcoded strings for config values
- [ ] Prefer immutability for DTOs — use Lombok `@Value` (immutable) where possible

### Exception handling
- [ ] Services throw standard Java exceptions only: `NoSuchElementException`, `IllegalArgumentException`, `IllegalStateException`
- [ ] No `ResponseStatusException` or HTTP-specific exceptions inside services
- [ ] All HTTP error mapping is centralized in a `@RestControllerAdvice`

### Security
- [ ] Endpoints secured with `@PreAuthorize` annotations
- [ ] No sensitive data (tokens, passwords) logged or returned in responses
- [ ] Input validated with Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) on DTOs

### Testing
- [ ] Unit tests use JUnit 5 + Mockito
- [ ] Integration tests use `@SpringBootTest` + Testcontainers (PostgreSQL) or `@DataJpaTest` (H2)
- [ ] Test method names follow the convention: `methodName` / `methodNameWithStateUnderTestExpectedBehavior`
  - Good: `getFormByIdWithUnknownIdThrowsException`
  - Bad: `testGetForm`, `should_return_404`
- [ ] JUnit 5 assertions used (`assertThat`, `assertEquals` from `org.junit.jupiter`)
- [ ] No Spring-specific exceptions leaking into service-layer test assertions

### Single Responsibility
- [ ] Each class/method has one reason to change
- [ ] Methods are small and do one thing; if > ~20 lines, check if it can be split
- [ ] No DRY violations — shared logic extracted to a utility or service method

---

## TypeScript / React checklist

### Type safety
- [ ] No `any` type used anywhere — use `unknown` + type guards, or define a proper interface/type
- [ ] All component props typed with an interface or type alias
- [ ] API response shapes typed and kept in sync with backend DTOs (`frontend/src/types/Form.ts`)

### Components & hooks
- [ ] Functional components only — no class components
- [ ] `const` by default; `let` only when reassignment is required
- [ ] Component filenames match the default export name (PascalCase)
- [ ] Hooks named with `use` prefix; not called conditionally

### Data fetching
- [ ] `useQuery` / `useMutation` from `@tanstack/react-query` used for all server state
- [ ] No direct `fetch` calls inside pages or components — all HTTP goes through `formClient` or a service module using `http.request`
- [ ] Query keys are stable arrays; queries guarded with `enabled` when required params are missing
- [ ] Mutations invalidate relevant query keys on success

### Auth & routing
- [ ] Auth state read via `useAuth` from `react-oidc-context`
- [ ] Token sourced from `user?.access_token` and passed to `formClient`
- [ ] Route definitions live in `src/components/Content.tsx`
- [ ] Unauthenticated flows use `signinRedirect`; unknown routes use `Navigate`

### Forms & validation
- [ ] User-entered forms use `react-hook-form`
- [ ] Validation schemas defined with Zod
- [ ] Dynamic form rendering reuses `DynamicForm` and the existing field components (`TextField`, `SelectField`, etc.)
- [ ] `FormField` and `FormValues` types kept aligned with backend DTOs

### i18n
- [ ] All user-visible strings go through `useTranslation` + `t('key.path')`
- [ ] No hardcoded user-visible strings in JSX (labels, placeholders, button text, error messages)
- [ ] Translation keys are hardcoded string literals — dynamic key construction (e.g. `` t(`section.${var}`) ``) is a violation
- [ ] Date formatting handled via i18n helpers (not inline `toLocaleDateString`)

### Style & accessibility
- [ ] `react-bootstrap` components and Bootstrap utility classes preferred over custom CSS
- [ ] Semantic HTML used; ARIA attributes added where needed
- [ ] No unnecessary dependencies added

### Testing
- [ ] Component tests written with Vitest + `@testing-library/react`
- [ ] `userEvent` preferred over `fireEvent` for interactions
- [ ] No implementation details tested — assert on what the user sees/can do

---

## Authoritative TypeScript sources

Use these references to justify findings and resolve ambiguity during TypeScript/React reviews.

### Official TypeScript sources
- TypeScript Do's and Don'ts (typescriptlang.org)
- TypeScript Performance Wiki (github.com/microsoft/TypeScript/wiki/Performance)
- Microsoft TypeScript Coding Guidelines

### Books
- *Programming TypeScript* by Boris Cherny (O'Reilly) — branded types, totality
- *Effective TypeScript*, 2nd Edition (2024) — adds `satisfies`, template literals, 25+ new items

### Style guides
- Google TypeScript Style Guide (gts) — bans `enum`/`namespace`/`any`
- Azure SDK Guidelines — rigorous public API rules

### Tooling
- `@typescript-eslint` strict + strict-type-checked rules — catches floating promises, any-infection, exhaustive switches
