package com.wonseok.wspb.annotation

/**
 * Marks a Kotlin class as a source for `.proto` generation.
 *
 * The processor reads [name] as the generated file name and also derives the
 * Protobuf message name from it. For example, `user_preference` becomes
 * `user_preference.proto` and `UserPreference`.
 */
@Target(AnnotationTarget.CLASS)
annotation class WSProto(val name: String)
