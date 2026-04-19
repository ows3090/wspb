# wspb

`wspb` generates Protobuf schemas and Java lite sources for Android projects from Kotlin models annotated with `@WSProto`.

```text
Kotlin model -> @WSProto -> KSP-generated .proto -> protobuf Java lite source
```

## Modules

- `wspb-annotation`: defines the `@WSProto` annotation.
- `wspb-processor`: KSP processor that generates `.proto` files.
- `wspb-gradle-plugin`: Android Gradle plugin that connects generated `.proto` files to the protobuf Gradle plugin.
- `local-sample-app`: sample app that consumes the local project modules directly.
- `published-sample-app`: sample app that consumes the published Maven coordinates, usually through `mavenLocal()` during local verification.

## Quick Start

Published artifacts:

- Gradle plugin: <https://plugins.gradle.org/plugin/io.github.ows3090.wspb.proto>
- Annotation artifact: <https://central.sonatype.com/artifact/io.github.ows3090/wspb-annotation>
- Processor artifact: <https://central.sonatype.com/artifact/io.github.ows3090/wspb-processor>

Apply the Android, Kotlin, KSP, and wspb plugins:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.github.ows3090.wspb.proto") version "1.0.3"
}
```

Add the annotation and processor:

```kotlin
dependencies {
    implementation("io.github.ows3090:wspb-annotation:1.0.3")
    ksp("io.github.ows3090:wspb-processor:1.0.3")
}
```

Declare a model:

```kotlin
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String,
)
```

Build the app. `wspb` generates:

- `user_preference.proto`
- `UserPreference` Java lite classes

The generated class can then be used from Kotlin or Java:

```kotlin
val userPreference = UserPreference.newBuilder()
    .setId(1)
    .setName("wonseok")
    .build()
```

## Configuration

Optional KSP arguments:

```kotlin
ksp {
    arg("wspb.proto.packagePath", "proto/com/example/schema")
    arg("wspb.proto.javaPackage", "com.example.schema")
    arg("wspb.processor.verbose", "true")
}
```

| Option | Default | Purpose |
| --- | --- | --- |
| `wspb.proto.packagePath` | `proto/com/wonseok/wspb` | Resource path where generated `.proto` files are written. |
| `wspb.proto.javaPackage` | `com.wonseok.wspb` | Value written to `option java_package` in generated `.proto` files. |
| `wspb.processor.verbose` | `false` | Enables extra processor logs. Accepts only `true` or `false`. |

## Type Mapping

| Kotlin type | Proto type |
| --- | --- |
| `Int`, `Short`, `Byte` | `int32` |
| `Long` | `int64` |
| `Float` | `float` |
| `Double` | `double` |
| `Boolean` | `bool` |
| `String` | `string` |
| `ByteArray` | `bytes` |
| `List<T>`, `Set<T>`, `Array<T>` | `repeated <mapped T>` |
| `Map<K, V>` | `map<key, value>` |

`Map` keys must be `Int`, `Short`, `Byte`, `Long`, `Boolean`, or `String`. `Map` values cannot be another collection or map.

## Naming Rules

- `@WSProto(name = "...")` must match `[a-z][a-z0-9_]*`.
- The annotation name becomes the `.proto` file name.
- Snake case annotation names become PascalCase message names. For example, `user_preference` becomes `UserPreference`.
- Kotlin property names become snake_case proto field names.
- The generated message name must be different from the Kotlin class name to avoid same-package class collisions.
- Each `@WSProto` name must be unique in a compilation.

## Generated Output

Default locations:

- KSP `.proto` files: `build/generated/ksp/<variant>/resources/proto/com/wonseok/wspb`
- Protobuf Java lite sources: `build/generated/sources/proto/<variant>/java`

If `wspb.proto.packagePath` is customized, the `.proto` subdirectory changes accordingly.

## Current Limitations

- Custom message types and enums are not supported yet.
- Nested collections are not supported.
- `Set<T>` is emitted as `repeated`, which does not enforce uniqueness in proto3.
- Field numbers are assigned from Kotlin property order; reordering properties changes the generated wire format.
- Nullable-specific schema behavior is not implemented yet.

## Compatibility

Current repository baseline:

- JDK 21
- Kotlin 2.0.21
- KSP 2.0.21-1.0.27
- Android Gradle Plugin 8.13.1
- Protobuf Gradle Plugin 0.9.5
- Protobuf 4.29.2

## Local Development

For contribution workflow and repository verification commands, see [Contributing](CONTRIBUTING.md).

## Documentation

- [Usage Guide](docs/USAGE.md)
- [Contributing](CONTRIBUTING.md)
