# Repository Guidelines

## Project Structure & Module Organization
Smart MyBatis is a Maven multi-module build: `core/` hosts the SQL abstraction layer, `spring-boot-starter-smart-mybatis/` wraps it as a Boot starter, and `spring-boot-starter-smart-mybatis-example/` contains an executable sample that wires the starter into a REST app. Source lives under `src/main/java` in each module, integration assets (mappers, YAML) go in `src/main/resources`, and all tests belong in `src/test/java`. Treat the example module as the quickest place to verify end-to-end behavior before releasing to Maven Central.

## Build, Test, and Development Commands
Use Maven from the repo root so dependency versions remain in sync.

```
mvn clean install            # compile all modules and run tests
mvn -pl core test            # run only the core contract tests
mvn -pl spring-boot-starter-smart-mybatis-example spring-boot:run
                             # boot the example service with the local profile
mvn -pl spring-boot-starter-smart-mybatis -am package
                             # package the starter along with its dependencies
```

## Coding Style & Naming Conventions
Target Java 17 and prefer four spaces for indentation. Follow MyBatis mapper idioms: XML mappers should mirror the package of their companion interface, and method names should state intent (`findUsersByEmail`, `insertOrderBatch`). Keep public classes and configuration properties in PascalCase, member fields in camelCase, and constants in UPPER_SNAKE_CASE. Use `@ConfigurationProperties` with kebab-case keys (`smart-mybatis.sql-cache.ttl`). Run `mvn fmt:format` (if configured locally) before pushing; otherwise ensure imports are ordered `java.*`, `javax.*`, third-party, then project packages.

## Testing Guidelines
JUnit-based tests live in the parallel `src/test/java` tree, mirroring the package of the class under test. Prefer descriptive names such as `SmartSqlSessionFactoryTest` and annotate integration tests with `@SpringBootTest` inside the starter module. Write tests for every mapper or plugin extension that touches SQL generation; mock data sources via H2 when MySQL-specific behavior is not required. Execute `mvn test` prior to opening a PR and add a regression test whenever you fix a bug or add a new annotation/mapper feature.

## Commit & Pull Request Guidelines
The history currently uses short, imperative subjects (e.g., “Initial commit”); keep that format with ≤72-character summaries and detailed bodies when a change spans modules. Reference GitHub issues via `Fixes #123` when applicable and keep commits scoped to a single concern (core SPI, starter auto-config, sample UX). Pull requests should describe the intent, affected modules, new config flags, and testing evidence (command output or screenshots of the example app). Link any breaking changes to migration notes and request review from a maintainer if you alter SQL interception, session factories, or starter auto-configuration.
