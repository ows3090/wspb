package com.wonseok.wspb.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for option parsing.
 *
 * These tests stay small on purpose because they validate pure parsing logic
 * without booting the full KSP compilation environment.
 */
class ProcessorOptionsTest {
    @Test
    fun `uses defaults when options are missing`() {
        val options = ProcessorOptions.from(emptyMap(), FakeLogger())

        assertEquals("proto/com/wonseok/wspb", options.protoPackagePath)
        assertEquals("com.wonseok.wspb", options.protoJavaPackage)
        assertFalse(options.verbose)
    }

    @Test
    fun `uses provided options when values are valid`() {
        val options = ProcessorOptions.from(
            options = mapOf(
                ProcessorOptions.PROTO_PACKAGE_PATH_KEY to " proto/com/example/schema ",
                ProcessorOptions.PROTO_JAVA_PACKAGE_KEY to " com.example.schema ",
                ProcessorOptions.VERBOSE_KEY to "true",
            ),
            logger = FakeLogger(),
        )

        assertEquals("proto/com/example/schema", options.protoPackagePath)
        assertEquals("com.example.schema", options.protoJavaPackage)
        assertTrue(options.verbose)
    }

    @Test
    fun `falls back to defaults when string options are blank`() {
        val logger = FakeLogger()
        val options = ProcessorOptions.from(
            options = mapOf(
                ProcessorOptions.PROTO_PACKAGE_PATH_KEY to "   ",
                ProcessorOptions.PROTO_JAVA_PACKAGE_KEY to "",
            ),
            logger = logger,
        )

        assertEquals("proto/com/wonseok/wspb", options.protoPackagePath)
        assertEquals("com.wonseok.wspb", options.protoJavaPackage)
        assertEquals(2, logger.warnings.size)
    }

    @Test
    fun `falls back to default when verbose value is invalid`() {
        val logger = FakeLogger()
        val options = ProcessorOptions.from(
            options = mapOf(ProcessorOptions.VERBOSE_KEY to "not_boolean"),
            logger = logger,
        )

        assertFalse(options.verbose)
        assertEquals(1, logger.warnings.size)
    }

    /**
     * Test double that records warnings so assertions can verify fallback
     * behavior without relying on compiler output text.
     */
    private class FakeLogger : KSPLogger {
        val warnings = mutableListOf<String>()

        override fun logging(message: String, symbol: KSNode?) = Unit

        override fun info(message: String, symbol: KSNode?) = Unit

        override fun warn(message: String, symbol: KSNode?) {
            warnings.add(message)
        }

        override fun error(message: String, symbol: KSNode?) = Unit

        override fun exception(e: Throwable) = Unit
    }
}
