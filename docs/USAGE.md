# Usage Guide

This guide is for Android apps and Android libraries that consume a published `wspb` release.

## 1. Install

Add the plugin:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.github.ows3090.wspb.proto") version "1.0.2"
}
```

For an Android library module, use `com.android.library` instead of `com.android.application`.

Add the dependencies:

```kotlin
dependencies {
    implementation("io.github.ows3090:wspb-annotation:1.0.2")
    ksp("io.github.ows3090:wspb-processor:1.0.2")
}
```

The wspb Gradle plugin automatically:

- Applies `com.google.protobuf`.
- Configures `protoc` for Java lite generation.
- Adds generated KSP resource directories as protobuf source directories.
- Adds `com.google.protobuf:protobuf-kotlin-lite`.

## 2. Declare Models

Annotate concrete Kotlin classes:

```kotlin
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String,
    val isAdmin: Boolean,
)
```

Build the module. The generated message name is derived from the annotation name:

```kotlin
val userPreference = UserPreference.newBuilder()
    .setId(1)
    .setName("wonseok")
    .setIsAdmin(true)
    .build()
```

## 3. Configure Output Packages

The default generated proto package path is `proto/com/wonseok/wspb`, and the default generated Java package is `com.wonseok.wspb`.

Customize them with KSP arguments:

```kotlin
ksp {
    arg("wspb.proto.packagePath", "proto/com/example/schema")
    arg("wspb.proto.javaPackage", "com.example.schema")
}
```

With this configuration:

- `.proto` files are written under `build/generated/ksp/<variant>/resources/proto/com/example/schema`.
- Generated Java lite classes use `com.example.schema`.

Enable troubleshooting logs only when needed:

```kotlin
ksp {
    arg("wspb.processor.verbose", "true")
}
```

## 4. Supported Types

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

`Map` key types are limited to `Int`, `Short`, `Byte`, `Long`, `Boolean`, and `String`. `Map` values may use any supported non-collection value type, including `ByteArray`.

Example:

```kotlin
@WSProto(name = "profile_cache")
data class ProfileCacheData(
    val userId: Long,
    val tags: List<String>,
    val attributes: Map<String, String>,
)
```

Generated schema shape:

```proto
syntax = "proto3";

option java_package = "com.wonseok.wspb";
option java_multiple_files = true;

message ProfileCache {
    int64 user_id = 1;
    repeated string tags = 2;
    map<string, string> attributes = 3;
}
```

## 5. Naming Rules

Use stable snake_case names:

```kotlin
@WSProto(name = "profile_cache")
```

Rules:

- The name must match `[a-z][a-z0-9_]*`.
- The generated file is `<name>.proto`.
- The generated message is PascalCase. `profile_cache` becomes `ProfileCache`.
- Kotlin properties are converted to snake_case field names.
- Field numbers are assigned from Kotlin property order, so do not reorder persisted model properties after release.
- Do not use an annotation name that produces the same name as the Kotlin class. `@WSProto(name = "user_data") data class UserData` is rejected.
- Do not reuse the same `@WSProto` name in one compilation.

## 6. Common Failures

`Unsupported type: Address`

Use only the supported types listed above. Custom message types are not implemented yet.

`Nested collections are not supported in proto3`

Replace nested collections such as `List<List<String>>` with a flat supported type or model the relationship outside wspb for now.

`Proto3 map key must be an integral or string type`

Use `Int`, `Short`, `Byte`, `Long`, `Boolean`, or `String` for the `Map` key.

`Class Name must be different from file name`

Rename either the Kotlin class or the `@WSProto` name so the generated message class does not collide with the model class.
