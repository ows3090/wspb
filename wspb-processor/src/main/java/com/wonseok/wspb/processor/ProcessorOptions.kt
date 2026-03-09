package com.wonseok.wspb.processor

import com.google.devtools.ksp.processing.KSPLogger

internal data class ProcessorOptions(
    val protoPackagePath: String,
    val protoJavaPackage: String,
    val verbose: Boolean,
) {
    companion object {
        const val PROTO_PACKAGE_PATH_KEY = "wspb.proto.packagePath"
        const val PROTO_JAVA_PACKAGE_KEY = "wspb.proto.javaPackage"
        const val VERBOSE_KEY = "wspb.processor.verbose"

        private const val DEFAULT_PROTO_PACKAGE_PATH = "proto/com/wonseok/wspb"
        private const val DEFAULT_PROTO_JAVA_PACKAGE = "com.wonseok.wspb"
        private const val DEFAULT_VERBOSE = false

        fun from(options: Map<String, String>, logger: KSPLogger): ProcessorOptions = ProcessorOptions(
            protoPackagePath = readStringOption(
                key = PROTO_PACKAGE_PATH_KEY,
                default = DEFAULT_PROTO_PACKAGE_PATH,
                options = options,
                logger = logger,
            ),
            protoJavaPackage = readStringOption(
                key = PROTO_JAVA_PACKAGE_KEY,
                default = DEFAULT_PROTO_JAVA_PACKAGE,
                options = options,
                logger = logger,
            ),
            verbose = readBooleanOption(
                key = VERBOSE_KEY,
                default = DEFAULT_VERBOSE,
                options = options,
                logger = logger,
            ),
        )

        private fun readStringOption(
            key: String,
            default: String,
            options: Map<String, String>,
            logger: KSPLogger,
        ): String {
            val rawValue = options[key] ?: return default
            val value = rawValue.trim()
            if (value.isEmpty()) {
                logger.warn("[WSPB] Option '$key' is blank. Falling back to default '$default'.")
                return default
            }
            return value
        }

        private fun readBooleanOption(
            key: String,
            default: Boolean,
            options: Map<String, String>,
            logger: KSPLogger,
        ): Boolean {
            val rawValue = options[key] ?: return default
            val value = rawValue.trim()
            if (value.isEmpty()) {
                logger.warn("[WSPB] Option '$key' is blank. Falling back to default '$default'.")
                return default
            }

            val parsed = value.lowercase().toBooleanStrictOrNull()
            if (parsed == null) {
                logger.warn("[WSPB] Option '$key' has invalid boolean value '$rawValue'. Falling back to default '$default'.")
                return default
            }
            return parsed
        }
    }
}
