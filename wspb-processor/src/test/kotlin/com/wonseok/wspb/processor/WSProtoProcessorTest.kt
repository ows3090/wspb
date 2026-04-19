package com.wonseok.wspb.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end tests for the processor using in-memory compilation.
 *
 * This style of test is valuable for code generators because it verifies the
 * same contract that real consumers rely on: source in, compilation outcome and
 * generated files out.
 */
@OptIn(ExperimentalCompilerApi::class)
class WSProtoProcessorTest {
    @Test
    fun `generates proto for supported fields and naming rules`() {
        val outcome = compile(
            SourceFile.kotlin(
                "UserData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "user_preference")
                data class UserData(
                    val userName: String,
                    val id: Int,
                    val isAdmin: Boolean,
                    val avatar: ByteArray,
                    val scores: List<Long>,
                    val aliases: Set<String>,
                    val weights: Array<Double>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode)

        val generatedProto = outcome.requireGeneratedProto("user_preference.proto")
        assertTrue(generatedProto.contains("message UserPreference"))
        assertTrue(generatedProto.contains("string user_name = 1;"))
        assertTrue(generatedProto.contains("int32 id = 2;"))
        assertTrue(generatedProto.contains("bool is_admin = 3;"))
        assertTrue(generatedProto.contains("bytes avatar = 4;"))
        assertTrue(generatedProto.contains("repeated int64 scores = 5;"))
        assertTrue(generatedProto.contains("repeated string aliases = 6;"))
        assertTrue(generatedProto.contains("repeated double weights = 7;"))
    }

    @Test
    fun `converts consecutive uppercase property names to correct snake_case`() {
        val outcome = compile(
            SourceFile.kotlin(
                "AcronymData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "acronym_test")
                data class AcronymData(
                    val userURL: String,
                    val isHTTPSEnabled: Boolean,
                    val ioError: String,
                    val simpleId: Int,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode)

        val generatedProto = outcome.requireGeneratedProto("acronym_test.proto")
        assertTrue(generatedProto.contains("string user_url = 1;"))
        assertTrue(generatedProto.contains("bool is_https_enabled = 2;"))
        assertTrue(generatedProto.contains("string io_error = 3;"))
        assertTrue(generatedProto.contains("int32 simple_id = 4;"))
    }

    @Test
    fun `applies custom processor options to generated proto`() {
        val outcome = compile(
            SourceFile.kotlin(
                "ConfiguredData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "configured_message")
                data class ConfiguredData(
                    val createdAt: Long,
                )
                """.trimIndent(),
            ),
            kspArgs = mapOf(
                ProcessorOptions.PROTO_PACKAGE_PATH_KEY to "proto/com/example/schema",
                ProcessorOptions.PROTO_JAVA_PACKAGE_KEY to "com.example.schema",
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode)

        val generatedProto = outcome.requireGeneratedProto("configured_message.proto")
        assertTrue(generatedProto.contains("""option java_package = "com.example.schema";"""))
        assertTrue(generatedProto.contains("int64 created_at = 1;"))
        assertTrue(
            outcome.compilation.kspSourcesDir
                .walkTopDown()
                .any { it.isFile && it.invariantSeparatorsPath.endsWith("resources/proto/com/example/schema/configured_message.proto") },
        )
    }

    @Test
    fun `fails for unsupported custom field types`() {
        val outcome = compile(
            SourceFile.kotlin(
                "UnsupportedData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                data class Address(
                    val city: String,
                )

                @WSProto(name = "user_preference")
                data class UserData(
                    val address: Address,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Unsupported type: Address"))
        assertFalse(outcome.hasGeneratedProto("user_preference.proto"))
    }

    @Test
    fun `warns when Set type is used`() {
        val outcome = compile(
            SourceFile.kotlin(
                "SetData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "set_test")
                data class SetData(
                    val tags: Set<String>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Set<T> is mapped to 'repeated' which allows duplicates in proto3"))

        val generatedProto = outcome.requireGeneratedProto("set_test.proto")
        assertTrue(generatedProto.contains("repeated string tags = 1;"))
    }

    @Test
    fun `generates proto for Map fields`() {
        val outcome = compile(
            SourceFile.kotlin(
                "MapData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "map_test")
                data class MapData(
                    val tags: Map<String, Int>,
                    val metadata: Map<Int, String>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, outcome.result.exitCode)

        val generatedProto = outcome.requireGeneratedProto("map_test.proto")
        assertTrue(generatedProto.contains("map<string, int32> tags = 1;"))
        assertTrue(generatedProto.contains("map<int32, string> metadata = 2;"))
    }

    @Test
    fun `fails for Map with invalid key type`() {
        val outcome = compile(
            SourceFile.kotlin(
                "InvalidMapData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "invalid_map_test")
                data class InvalidMapData(
                    val data: Map<Double, String>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Proto3 map key must be an integral or string type"))
        assertFalse(outcome.hasGeneratedProto("invalid_map_test.proto"))
    }

    @Test
    fun `fails for repeated Map fields`() {
        val outcome = compile(
            SourceFile.kotlin(
                "RepeatedMapData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "repeated_map_test")
                data class RepeatedMapData(
                    val entries: List<Map<String, Int>>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Nested collections are not supported in proto3"))
        assertFalse(outcome.hasGeneratedProto("repeated_map_test.proto"))
    }

    @Test
    fun `fails for nested collections`() {
        val outcome = compile(
            SourceFile.kotlin(
                "NestedData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "nested_test")
                data class NestedData(
                    val matrix: List<List<String>>,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Nested collections are not supported in proto3"))
        assertFalse(outcome.hasGeneratedProto("nested_test.proto"))
    }

    @Test
    fun `fails when message name matches class name`() {
        val outcome = compile(
            SourceFile.kotlin(
                "UserData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "user_data")
                data class UserData(
                    val id: Int,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Class Name must be different from file name"))
        assertFalse(outcome.hasGeneratedProto("user_data.proto"))
    }

    @Test
    fun `fails for duplicate WSProto names`() {
        val outcome = compile(
            SourceFile.kotlin(
                "DuplicateData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "user_preference")
                data class UserDataA(
                    val id: Int,
                )

                @WSProto(name = "user_preference")
                data class UserDataB(
                    val name: String,
                )
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, outcome.result.exitCode)
        assertTrue(outcome.result.messages.contains("Duplicate @WSProto name 'user_preference'"))
    }

    @Test
    fun `fails for invalid WSProto name format`() {
        val invalidNames = listOf("" to "empty", "user preference" to "space", "user-data" to "hyphen", "123_bad" to "digit_start")
        for ((name, label) in invalidNames) {
            val outcome = compile(
                SourceFile.kotlin(
                    "InvalidName_$label.kt",
                    """
                    package test

                    import com.wonseok.wspb.annotation.WSProto

                    @WSProto(name = "$name")
                    data class InvalidName${label.replaceFirstChar { it.uppercase() }}(
                        val id: Int,
                    )
                    """.trimIndent(),
                ),
            )

            assertEquals(
                "Expected COMPILATION_ERROR for name='$name'",
                KotlinCompilation.ExitCode.COMPILATION_ERROR,
                outcome.result.exitCode,
            )
            assertTrue(
                "Expected validation error for name='$name'",
                outcome.result.messages.contains("@WSProto name must match pattern"),
            )
        }
    }

    @Test
    fun `defers invalid symbols instead of generating proto`() {
        val outcome = compile(
            SourceFile.kotlin(
                "DeferredData.kt",
                """
                package test

                import com.wonseok.wspb.annotation.WSProto

                @WSProto(name = "deferred_message")
                data class DeferredData(
                    val missing: MissingType,
                )
                """.trimIndent(),
            ),
        )

        assertFalse(outcome.result.messages.contains("Unsupported type: MissingType"))
        assertFalse(outcome.hasGeneratedProto("deferred_message.proto"))
    }

    private fun compile(
        vararg sources: SourceFile,
        kspArgs: Map<String, String> = emptyMap(),
    ): CompilationOutcome {
        // The compile-testing library creates a temporary throwaway compilation
        // so the processor can be exercised without creating a real sample
        // project on disk.
        val compilation = KotlinCompilation().apply {
            // The generated source snippets passed to the helper become the
            // complete source set for this synthetic compilation.
            this.sources = sources.toList()
            // Register the real processor provider so the test follows the same
            // creation path as a normal KSP build.
            symbolProcessorProviders = listOf(WSProtoProcessorProvider())
            // Reuse the current test runtime classpath so the temporary
            // compilation can see `@WSProto`, Kotlin stdlib, and test
            // dependencies without building a separate Gradle project.
            inheritClassPath = true
            // Print compiler diagnostics to the test output to make failures
            // easier to diagnose in CI.
            messageOutputStream = System.out
            // Allow each test to simulate custom build-script KSP arguments.
            this.kspArgs.putAll(kspArgs)
        }

        return CompilationOutcome(
            compilation = compilation,
            result = compilation.compile(),
        )
    }

    /**
     * Wraps both the compilation handle and result so helper methods can inspect
     * generated files after the compilation finishes.
     */
    private data class CompilationOutcome(
        val compilation: KotlinCompilation,
        val result: KotlinCompilation.Result,
    ) {
        fun hasGeneratedProto(fileName: String): Boolean = compilation.kspSourcesDir.walkTopDown().any { it.isFile && it.name == fileName }

        fun requireGeneratedProto(fileName: String): String {
            val file = compilation.kspSourcesDir.walkTopDown().firstOrNull { it.isFile && it.name == fileName }
            assertNotNull("Generated proto '$fileName' was not found under ${compilation.kspSourcesDir}.", file)
            return file!!.readText()
        }
    }
}
