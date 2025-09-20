# Repository Guidelines

## Project Structure & Modules
- Backend code sits in `src/main/java/com/example/nomodel`. `_core` holds shared config, security, logging, and AOP. Domain modules (`member`, `model`, `report`, `coupon`, `subscription`, `file`, `point`, `statistics`) keep isolated `application`, `domain`, and `infrastructure` packages.
- Resources live in `src/main/resources`; API specs in `src/docs/asciidoc`. Tests mirror production packages in `src/test/java`, with fixtures under `_core` and SQL seeds in `src/test/resources/sql`. k6 scenarios and utilities live in `k6/`.

## Build, Test, and Documentation
- `./gradlew clean build` compiles, runs unit and integration suites, and regenerates REST Docs in `build/generated-snippets` and `build/docs`.
- Start the service via `./gradlew bootRun`. Provision MySQL and Redis with `docker compose -f compose.yml up -d` after setting `.env`.
- Use `./gradlew asciidoctor` to refresh docs only. Run performance suites with `./k6/run-tests.sh smoke|load|stress|spike` and optional `--influxdb` or `--prometheus` flags.

## Coding Style & Naming
- Java 21 toolchain with four-space indentation; favor Lombok builders/records and constructor injection already present.
- Keep services under `*.application.service`, DTOs under `*.application.dto`, aggregates in `*.domain.model`, repositories in `*.domain.repository`, schedulers in `*.infrastructure.scheduler`.
- Name controllers `*Controller`, configs `*Config`, and aspects within `_core/aop`. Centralize logging through `StructuredLogger` and reuse `_core/utils` helpers.

## Testing Expectations
- Use JUnit 5 and Spring Boot test slices. Extend `BaseUnitTest` or `BaseIntegrationTest`, leverage `WithMockTestUser`, and load data with SQL scripts when hitting repositories.
- Execute `./gradlew test -Dspring.profiles.active=test` before pushing; Testcontainers brings up MySQL, Redis, Kafka, and Elasticsearch—avoid manual dependency setup in CI.
- Place new files as `*Test` or `*IntegrationTest`, and capture REST Docs snippets via `_core/restdocs` utilities.

## Commit & PR Workflow
- Follow the repository’s Conventional Commit prefixes (`feat:`, `fix:`, `refactor:`, `test:`, etc.) with imperative subjects under ~72 characters.
- Confirm `./gradlew build` and relevant k6 runs pass. PRs should summarize domain impact, link issues, list validation steps (tests, docs, compose), and flag configuration or secret changes.

## Environment & Security Notes
- Keep secrets out of git; populate `.env` before running Docker services. Update `SecurityConfig` and related auth beans together, documenting new public or actuator paths in your PR so ops can adjust monitors.
