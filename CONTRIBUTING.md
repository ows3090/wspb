# Contributing

## Principles

- Keep changes small, explicit, and easy to review.
- Update documentation when behavior, configuration, or user-facing examples change.
- Change library behavior in the core modules first: `wspb-annotation`, `wspb-processor`, and `wspb-gradle-plugin`.
- Keep `local-sample-app` and `published-sample-app` as verification harnesses, not as places for library logic.

## Development Flow

1. Create a focused branch.
2. Make the code or documentation change.
3. Run the smallest useful verification command.
4. Run the broader checks before opening a pull request.
5. Open a pull request with the change scope and verification results.

## Verification

Use focused checks during development:

```bash
./gradlew :wspb-processor:test
./gradlew :local-sample-app:assembleDebug
```

Use the full repository check before review:

```bash
./gradlew publishToMavenLocal --configure-on-demand
./gradlew :local-sample-app:assembleDebug
./gradlew :published-sample-app:assembleDebug
./gradlew spotlessCheck
./gradlew lint
```

Run `publishToMavenLocal --configure-on-demand` before checking `published-sample-app` so the sample resolves the latest local artifacts.

## Commit Guide

- Prefer small commits that each explain one intent.
- Use imperative commit messages.
- Example: `Add proto mapping for ByteArray`.

## Pull Request Guide

- Fill out the pull request template.
- Describe the changed behavior and why it changed.
- Include the exact Gradle commands you ran.
- For breaking changes, include the impact and migration path.

## Issue Reports

Include:

- Minimal reproduction steps.
- Environment details: JDK, Gradle, Android Gradle Plugin, Kotlin, and KSP versions.
- Relevant logs or stack traces.
- Expected behavior and actual behavior.
