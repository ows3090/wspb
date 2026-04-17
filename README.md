# wspb

`wspb` generates `.proto` schemas and Protobuf lite sources from Kotlin models by combining an annotation, a KSP processor, and an Android Gradle plugin.

Kotlin model -> `@WSProto` -> KSP `.proto` generation -> Android proto source wiring -> `protoc` Java lite sources

## Modules

- `wspb-annotation`: defines `@WSProto`
- `wspb-processor`: KSP processor that generates `.proto` files
- `wspb-gradle-plugin`: Android Gradle plugin that wires generated proto directories into the build
- `local-sample-app`: local-development consumer app using project modules
- `published-sample-app`: consumer app using published Maven coordinates

## Planned Public Coordinates

These are the coordinates intended for published releases:

```kotlin
dependencies {
    implementation("io.github.ows3090:wspb-annotation:1.0.1")
    ksp("io.github.ows3090:wspb-processor:1.0.1")
}
```

```kotlin
plugins {
    id("io.github.ows3090.wspb.proto") version "1.0.1"
}
```

## Quick Start

The example below shows the intended public-consumer setup after the first release is published.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.github.ows3090.wspb.proto") version "1.0.1"
}

dependencies {
    implementation("io.github.ows3090:wspb-annotation:1.0.1")
    ksp("io.github.ows3090:wspb-processor:1.0.1")
}
```

```kotlin
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String,
)
```

## KSP Options

```kotlin
ksp {
    arg("wspb.proto.packagePath", "proto/com/example/schema")
    arg("wspb.proto.javaPackage", "com.example.schema")
    arg("wspb.processor.verbose", "true")
}
```

- `wspb.proto.packagePath`: output subdirectory for generated `.proto` files
- `wspb.proto.javaPackage`: `option java_package` written into generated `.proto` files
- `wspb.processor.verbose`: enables processor warning logs

Default values:

- `wspb.proto.packagePath = proto/com/wonseok/wspb`
- `wspb.proto.javaPackage = com.wonseok.wspb`
- `wspb.processor.verbose = false`

## Compatibility

Current repo baseline:

- JDK 21
- Kotlin 2.0.21
- KSP 2.0.21-1.0.27
- AGP 8.13.1

These are the versions currently validated in this repository. A broader compatibility policy will be documented after the first public release.

## Limitations

- Supported field types are limited to primitives, `String`, `ByteArray`, `List`, `Set`, and `Array`
- Custom classes, enums, and nullable-specific mapping are not yet supported
- The generated proto path defaults to the repository package unless overridden with KSP options

## Local Development

Inside this repository, the safest verification flow is:

```bash
./gradlew publishToMavenLocal --configure-on-demand
./gradlew :local-sample-app:assembleDebug
./gradlew :published-sample-app:assembleDebug
./gradlew :wspb-processor:test
./gradlew spotlessCheck
./gradlew lint
```

`local-sample-app` and `published-sample-app` can both be opened and built from Android Studio as separate app modules. The former validates project-module consumption, and the latter validates the published dependency flow through `mavenLocal()` or remote repositories.

## Docs

- [Usage Guide](docs/USAGE.md)
- [Contributing](CONTRIBUTING.md)
