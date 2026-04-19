package com.wonseok.wspb.processor

import com.google.devtools.ksp.processing.KSPLogger

/**
 * Normalized KSP options used by the processor.
 *
 * KSP exposes all arguments as strings, so this type centralizes parsing,
 * validation, and default handling before the rest of the processor consumes
 * the values.
 */
internal data class ProcessorOptions(
    /** Resource path where generated `.proto` files should be written. */
    val protoPackagePath: String,
    /** Value written to `option java_package` inside generated `.proto` files. */
    val protoJavaPackage: String,
    /** Enables extra processor logging when troubleshooting generation issues. */
    val verbose: Boolean,
) {
    companion object {
        /** KSP argument for the generated `.proto` resource path. */
        const val PROTO_PACKAGE_PATH_KEY = "wspb.proto.packagePath"

        /** KSP argument for the generated Protobuf Java package. */
        const val PROTO_JAVA_PACKAGE_KEY = "wspb.proto.javaPackage"

        /** KSP argument that turns verbose processor logs on or off. */
        const val VERBOSE_KEY = "wspb.processor.verbose"

        private const val DEFAULT_PROTO_PACKAGE_PATH = "proto/com/wonseok/wspb"
        private const val DEFAULT_PROTO_JAVA_PACKAGE = "com.wonseok.wspb"
        private const val DEFAULT_VERBOSE = false

        /**
         * Converts the raw KSP option map into strongly typed processor settings.
         *
         * Invalid user input does not stop compilation here. Instead, the processor
         * logs a warning and falls back to a safe default value.
         */
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
                // Blank string values are almost always configuration mistakes.
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

            // `toBooleanStrictOrNull` accepts only `true` or `false`, which keeps
            // the contract explicit for build script authors.
            val parsed = value.lowercase().toBooleanStrictOrNull()
            if (parsed == null) {
                logger.warn("[WSPB] Option '$key' has invalid boolean value '$rawValue'. Falling back to default '$default'.")
                return default
            }
            return parsed
        }
    }
}
