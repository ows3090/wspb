# Usage Guide

## Audience

This guide is for Android projects consuming a published `wspb` release.

If you are working inside this repository itself, use the local development flow from [README.md](../README.md) instead.

## Installation

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

The snippets above describe the intended public-consumer setup. Until the first public release is published, those coordinates are not yet available from a remote repository.

## Declare a Model

```kotlin
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String,
)
```

## Optional KSP Configuration

```kotlin
ksp {
    arg("wspb.proto.packagePath", "proto/com/example/schema")
    arg("wspb.proto.javaPackage", "com.example.schema")
    arg("wspb.processor.verbose", "true")
}
```

Option reference:

- `wspb.proto.packagePath`: generated `.proto` output subdirectory
- `wspb.proto.javaPackage`: `option java_package` value written into generated `.proto` files
- `wspb.processor.verbose`: enables processor warning logs

Defaults:

- `wspb.proto.packagePath = proto/com/wonseok/wspb`
- `wspb.proto.javaPackage = com.wonseok.wspb`
- `wspb.processor.verbose = false`

## Generated Output

Default output locations:

- KSP `.proto` files: `build/generated/ksp/<variant>/resources/proto/com/wonseok/wspb`
- Protobuf Java lite sources: `build/generated/sources/proto/<variant>/java`

If you override `wspb.proto.packagePath`, the generated `.proto` subdirectory changes accordingly.

## Type Mapping

- `Int`, `Short`, `Byte` -> `int32`
- `Long` -> `int64`
- `Float` -> `float`
- `Double` -> `double`
- `Boolean` -> `bool`
- `String` -> `string`
- `ByteArray` -> `bytes`
- `List<T>`, `Set<T>`, `Array<T>` -> `repeated <mapped(T)>`

## Naming Rules

- File name: uses `@WSProto(name = "...")`
- Message name: converts `snake_case` into `PascalCase`
- Field name: converts Kotlin property names into `snake_case`

## Current Limitations

- Unsupported field types fail the processor
- Message names that match the annotated class name are rejected
- Enum, custom message, and richer nullable support are not implemented yet

## Compatibility Baseline

Repository-validated baseline:

- JDK 21
- Kotlin 2.0.21
- KSP 2.0.21-1.0.27
- AGP 8.13.1
