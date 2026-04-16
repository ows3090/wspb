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
        // compile-testing 라이브러리의 in-memory 컴파일 환경
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList() // 테스트 소스들을 컴파일 입력
            symbolProcessorProviders = listOf(WSProtoProcessorProvider())   // 테스트 컴파일에 우리 KSP processor를 연결
            inheritClassPath = true     // 테스트용 컴파일이 현재 프로젝트의 의존성/classpath를 같이 사용
            messageOutputStream = System.out    // 컴파일 로그를 표준 출력
            this.kspArgs.putAll(kspArgs)    // KSP 옵션을 이 컴파일에 주입
        }

        return CompilationOutcome(
            compilation = compilation,
            result = compilation.compile(),
        )
    }

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
