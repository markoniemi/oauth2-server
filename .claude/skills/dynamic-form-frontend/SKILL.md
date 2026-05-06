---
name: dynamic-form-frontend
description: "Work on the dynamic-form React/Vite frontend (React 19 + TypeScript). Use when editing frontend UI, routing, auth/OIDC, react-query data fetching, react-hook-form + zod validation, i18n, or Bootstrap-based components under frontend/."
---

# Dynamic Form Frontend

## Scope and defaults

- Work inside frontend/ for UI changes; keep backend untouched unless asked.
- Use React function components and hooks only.
- Use TypeScript types from frontend/src/types first; extend them when needed.
- Prefer react-bootstrap components and Bootstrap utility classes.

## Project map (frontend/)

- src/App.tsx: Router + AuthProvider.
- src/main.tsx: QueryClientProvider + app bootstrap.
- src/context/oidcConfig.tsx: OIDC client config.
- src/services/http.ts: API wrapper; use for fetch.
- src/services/formClient.ts: Form and submission API calls.
- src/components/: reusable UI and dynamic form field controls.
- src/pages/: route-level screens.
- src/i18n.ts: i18next config and date formatting.

## Data access and caching

- Use @tanstack/react-query for reads and writes.
- For queries, set stable queryKey arrays; enable only when required params exist.
- For mutations, call formClient methods and invalidate related queryKey values on success.
- Do not call fetch directly in pages; extend formClient or add a new service wrapper that uses http.request.

## Auth and routing

- Read auth state with useAuth from react-oidc-context.
- Pull the token from user?.access_token and pass it to formClient.
- Keep route definitions in src/components/Content.tsx.
- Use Navigate for unknown routes and handle unauthenticated flows with signinRedirect.

## Forms and validation

- For user-entered forms, use react-hook-form; pair with zod when adding new validation.
- For dynamic form rendering, reuse DynamicForm + field components (TextField, SelectField, etc.).
- For read-only views, use ReadOnlyDynamicForm.
- Keep FormField and FormValues aligned with backend DTOs; update frontend/src/types/Form.ts when schemas change.

## i18n

- No hardcoded user-visible strings in JSX or component logic — wrap every UI string with `t('key.path')`.
- Translation keys must be hardcoded string literals (e.g. `t('navigation.createForm')`). Dynamic key construction (e.g. `t(\`section.${var}\``) breaks static extraction tools like i18next-parser and typed-i18n plugins.
- Keep date formatting in i18n (see common.date.long usage).

## Testing

- Write component tests with Vitest + @testing-library/react.
- Prefer user-event for interactions.

## When adding a new page

1. Create a component in src/pages.
2. Wire it into Content routes.
3. Add required service calls in formClient or a new service module using http.request.
4. Add i18n keys for new user-facing strings.

---

## Effective TypeScript (Dan Vanderkam)

### Understanding the Type System
- **Know the difference between type and runtime** (Item 1): TypeScript types are erased at runtime. Never write code that assumes a type annotation enforces a runtime contract — validate at boundaries (API responses, user input).
- **Avoid `any` — use `unknown` for truly unknown values** (Item 5): `any` silently disables type checking. Use `unknown` and narrow with type guards. If `any` seems necessary, use `as unknown as T` only at well-understood boundaries and document why.
- **Prefer type annotations over type assertions** (Item 9): Annotate variables at declaration (`const x: Foo = ...`) rather than asserting after the fact (`... as Foo`). Assertions hide bugs; annotations catch them.
- **Avoid object wrapper types** (Item 10): Use `string`, `number`, `boolean` — not `String`, `Number`, `Boolean`.

### Type Design
- **Use type aliases to make types self-documenting** (Item 12): `type UserId = string` is clearer than bare `string` everywhere.
- **Apply excess property checking when creating objects** (Item 11): Assign object literals directly to typed variables to benefit from excess property checking; don't route through intermediate variables to avoid it.
- **Prefer union types over optional properties for mutually exclusive states** (Item 28 / Item 32): Model state explicitly — `{ state: 'loading' } | { state: 'error'; error: Error } | { state: 'ok'; data: T }` is clearer and safer than a single interface with many optional fields.
- **Use `readonly` to prevent accidental mutation** (Item 17): Mark function parameters `readonly` when you don't intend to mutate them; use `Readonly<T>` for data objects.
- **Prefer interfaces for public API shapes; types for computed/union types** (Item 13): Use `interface` for object shapes that may be extended; use `type` for unions, intersections, and mapped types.
- **Use mapped types to keep related types in sync** (Item 14): If two types must mirror each other, derive one from the other with `keyof`, `Pick`, `Omit`, or a mapped type rather than maintaining them separately.

### Working with `any`
- **Use the narrowest possible scope for `any`** (Item 38): If you must use `any`, confine it to the smallest expression possible — don't widen a whole variable's type to `any`.
- **Never return `any` from a function** (Item 39): A function returning `any` infects callers. Return a specific type or `unknown`.
- **Prefer type-safe alternatives to `any`** (Item 40): Use generics, union types, or `unknown` + narrowing before reaching for `any`.

### Generics and Type-Level Programming
- **Use generics to eliminate repetition** (Item 14): If you find yourself copying a type and changing one field, use a generic instead.
- **Use `keyof typeof` and conditional types carefully** (Item 50): Powerful but complex — only use advanced type-level programming if it genuinely removes duplication or improves safety.
- **Prefer generic constraints over `any`** (Item 14): `<T extends object>` is safer than `any` when you need flexibility.

### Type Declarations and `.d.ts`
- **Put types where they are used** (Item 8): Co-locate types with the code that uses them. Shared types live in `src/types/`; component-specific types can live near the component.
- **Export types that appear in public function signatures** (Item 47): If a function's parameter or return type is a named type, export it so callers can reference it without re-declaring.

### Strict Mode
- **Enable strict mode and treat its errors as bugs** (Item 2): All new code must compile cleanly under `strict: true`. Do not suppress errors with `// @ts-ignore` unless there is an explicit, documented reason.
- **Use `noUncheckedIndexedAccess` for arrays and records** (Item 48 spirit): Array access (`arr[i]`) can be `undefined` at runtime. Handle the undefined case explicitly rather than asserting non-null.

---

## React Official Rules (React 19)

### Component Purity
- **Components must be pure functions of their props and state** (React docs — Keeping Components Pure): Given the same inputs, a component must always return the same JSX. Never produce side effects during rendering (no writes to external variables, no API calls, no DOM mutations).
- **Do not mutate props or state directly**: Always produce new objects/arrays (`[...arr, item]` not `arr.push(item)`). React uses reference equality to detect changes.
- **Render must be side-effect free**: Move side effects into `useEffect`, event handlers, or server actions — never into the render body.

### Rules of Hooks
- **Only call hooks at the top level** (React docs — Rules of Hooks): Never call hooks inside loops, conditions, or nested functions. Hook call order must be identical on every render.
- **Only call hooks from React function components or custom hooks**: Never call hooks from plain JavaScript functions, class components, or event handlers.
- **Custom hooks must start with `use`**: This is enforced by the React linter. A function named `use*` signals to React (and linters) that it follows hook rules.
- **Each call to a hook gets isolated state**: Two components using the same custom hook each get their own independent state — hooks do not share state between components.

### State and Effects
- **Treat state as a snapshot**: State updates are asynchronous. Reading state immediately after `setState` returns the old value. Use the functional update form (`setState(prev => ...)`) when the new value depends on the previous one.
- **Declare all reactive dependencies in the `useEffect` dependency array**: Every value used inside `useEffect` that could change over time must be listed. Omitting dependencies causes stale closure bugs; the ESLint `exhaustive-deps` rule enforces this.
- **Clean up effects that set up subscriptions or timers**: Return a cleanup function from `useEffect` to avoid memory leaks and double-invocation bugs in Strict Mode.
- **Do not synchronize with effects when you can derive**: If a value can be computed from existing state or props during render, do not store it in state and sync it with `useEffect` — just compute it inline.
- **Avoid deeply nested state**: Flatten state structures. If you find yourself writing `setState(s => ({ ...s, a: { ...s.a, b: newVal } }))` regularly, restructure the state.

### Component Design
- **Keep components small and focused**: A component should do one thing. Extract sub-components when a component grows beyond a single screen of code.
- **Lift state only as high as necessary**: Place state in the lowest common ancestor of the components that need it. Avoid lifting to a global store prematurely.
- **Prefer controlled components for forms**: Use react-hook-form (already project standard) which manages controlled inputs — do not mix controlled and uncontrolled patterns in the same field.
- **Use `key` correctly on lists**: Keys must be stable, unique identifiers from your data (e.g., entity IDs) — not array indices. Wrong keys cause subtle UI bugs and broken animations.
- **Avoid `useEffect` for data fetching**: Use `@tanstack/react-query` (already project standard) instead of raw `useEffect` + `fetch`. React Query handles caching, deduplication, loading states, and error states correctly.

### StrictMode
- **Write components that tolerate double-invocation**: React 19 Strict Mode intentionally mounts, unmounts, and remounts components in development to surface cleanup bugs. Effects and component bodies must be idempotent.
