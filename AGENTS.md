# Repository Guidelines

## Purpose

This file defines the working rules for people and agents contributing to this repository.

## Repository Shape

Core modules:

- `wspb-annotation`
- `wspb-processor`
- `wspb-gradle-plugin`

Verification apps:

- `local-sample-app`: consumes the core modules through direct project dependencies.
- `published-sample-app`: consumes Maven/Gradle published coordinates, usually from `mavenLocal()` during local development.

## Working Principles

- Make behavior changes in the core modules.
- Use the sample apps to verify integration, not to hold library logic.
- Keep code and documentation aligned.
- Use the smallest command that validates the change you made.
- Write all Markdown documentation in English.

## Standard Verification Commands

```bash
./gradlew publishToMavenLocal --configure-on-demand
./gradlew :local-sample-app:assembleDebug
./gradlew :published-sample-app:assembleDebug
./gradlew spotlessCheck
./gradlew lint
```

For processor-only changes, `./gradlew :wspb-processor:test` is usually the fastest focused check.

## Documentation Map

- Overview and quick start: `README.md`
- Detailed usage: `docs/USAGE.md`
- Contribution workflow: `CONTRIBUTING.md`
