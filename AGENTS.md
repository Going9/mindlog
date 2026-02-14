# AI Agent Guidelines (AGENTS.md)

This file provides context and rules for AI agents working on the Mindlog codebase.
Follow these guidelines strictly to maintain consistency and code quality.

## 1. Project Overview
- **Stack**: Java 25, Spring Boot 4.0.1, Gradle
- **Frontend**: Thymeleaf (SSR), Tailwind CSS, Hotwire (Turbo Drive + Stimulus)
- **Database**: PostgreSQL, Spring Data JPA
- **Architecture**: Domain-Driven Design (DDD) like structure (`com.mindlog.domain.{feature}`)

## 2. Operational Commands

### Build & Run
- **Build**: `./gradlew build -x test` (Skip tests for quick build)
- **Run App**: `./gradlew bootRun`
- **Build CSS**: `npm run build-css` (Required for Tailwind changes)

### Testing
- **Run All Tests**: `./gradlew test`
- **Run Single Test**: `./gradlew test --tests "com.mindlog.path.to.TestClass"`
- **Run Specific Method**: `./gradlew test --tests "com.mindlog.path.to.TestClass.methodName"`

## 2-1. Git Workflow (Required)
- Do not implement directly on `main` or `release/*`.
- Start implementation on a work branch with one of these prefixes:
  - `feat/`, `fix/`, `refactor/`, `docs/`, `test/`, `chore/`, `perf/`, `build/`, `ci/`, `hotfix/`
- Merge order: `work branch -> release/* -> main`.
- Commit messages must follow Conventional Commits prefixes:
  - `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, `perf:`, `build:`, `ci:`, `revert:`
- Commit title/body language in this workspace: Korean.

## 3. Code Style & Conventions

### Java
- **Version**: Java 25 (Preview features may be enabled)
- **Var Usage**: Use `var` for local variables where type is obvious.
  ```java
  var diary = diaryService.getDiary(profileId, id); // Good
  ```
- **Indentation**: 4 spaces (Standard Java).
- **Imports**: Grouped: Project (`com.mindlog...`) -> Libraries -> Java/Javax.

### Architecture (Domain-Driven)
Place code in `src/main/java/com/mindlog/domain/{feature_name}`:
- `controller`: Web layer (`@Controller`).
- `service`: Business logic (`@Service`).
- `repository`: Data access (`@Repository` / JPA interfaces).
- `entity`: JPA entities (`@Entity`).
- `dto`: Data Transfer Objects (Records or Classes).

**Global Shared Code**: `src/main/java/com/mindlog/global/` (Config, Security, Common Utils).

### Controller Patterns (Hotwire/Turbo)
This project uses **Hotwire (Turbo Drive)**. Controllers must follow specific HTTP status rules:
- **Success (Post/Put/Delete)**: Must return `303 See Other` to redirect.
  ```java
  return new RedirectView("/path", true, false, false); // 303 Redirect
  ```
- **Form Validation Failure**: Must return `422 Unprocessable Entity` to re-render forms.
  ```java
  if (bindingResult.hasErrors()) {
      response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
      return "view/form";
  }
  ```
- **Context**: Use `@CurrentProfileId UUID profileId` to get the logged-in user context.

### Database / JPA
- **Timestamps**: Use `Instant` for time fields (`createdAt`, `updatedAt`).
- **Base Class**: Extend `BaseTimeEntity` for auditing fields.

### Language & Comments
- **Comments**: **Korean** is preferred for explaining complex business logic or architectural decisions.
- **JavaDoc**: Use for public API methods explaining "Why" and "How".

## 4. Frontend Guidelines (Thymeleaf + Tailwind)
- **CSS**: Use utility classes (Tailwind). Avoid custom CSS files if possible.
- **Layout**: Thymeleaf Layout Dialect is used (`layout:decorate`).

## 5. Development Checklist
1. **Check Turbo Compliance**: If adding a form, ensure 422/303 statuses are handled.
2. **Verify Auth**: Ensure `@CurrentProfileId` is used for user-scoped data.
3. **Run Tests**: Verify logic with `./gradlew test`.
4. **DTOs**: Use specific DTOs for requests/responses; do not expose Entities directly in Controller.

## 6. Request Review Policy (Required)
1. Before implementation, verify whether the request conflicts with platform/framework standards (Spring, Thymeleaf, Turbo, Stimulus, Hotwire Native).
2. If a more standard or operationally safer option exists, propose it first before coding.
3. If requested direction conflicts with the standard option, respond in this order:
   - conflict point
   - likely risk
   - recommended alternative
   - implementation path based on user choice
4. Do not execute requests blindly; guide toward maintainable and platform-aligned outcomes.
