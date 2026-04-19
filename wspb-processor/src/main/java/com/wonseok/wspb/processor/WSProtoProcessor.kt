package com.wonseok.wspb.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.wonseok.wspb.annotation.WSProto
import java.io.OutputStream

/**
 * KSP processor that converts annotated Kotlin classes into `.proto` files.
 *
 * The processor intentionally generates plain Protobuf schema files instead of
 * Kotlin or Java source. The Gradle plugin later wires those schema files into
 * the protobuf plugin so `protoc` can generate Java lite classes.
 */
class WSProtoProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    options: Map<String, String>,
) : SymbolProcessor {
    /** Parsed and validated processor options shared by the whole compilation. */
    private val processorOptions = ProcessorOptions.from(options, logger)

    /** Fully qualified annotation name used when asking KSP for annotated symbols. */
    private val annotationFqName = WSProto::class.qualifiedName ?: FALLBACK_ANNOTATION_FQ_NAME

    /** Tracks generated file names across KSP rounds to detect duplicates. */
    private val generatedNames = mutableSetOf<String>()

    companion object {
        private const val FALLBACK_ANNOTATION_FQ_NAME = "com.wonseok.wspb.annotation.WSProto"
        private val VALID_PROTO_NAME_PATTERN = Regex("^[a-z][a-z0-9_]*$")
        private val COLLECTION_TYPE_NAMES = setOf("List", "Set", "Array")
        private val VALID_MAP_KEY_TYPES = setOf("Int", "Short", "Byte", "Long", "Boolean", "String")
    }

    /** Writes optional trace logs when verbose mode is enabled from the build. */
    private fun verboseLog(message: String) {
        if (processorOptions.verbose) {
            logger.warn(message)
        }
    }

    /** Small convenience operator used when streaming generated file contents. */
    operator fun OutputStream.plusAssign(str: String) {
        this.write(str.toByteArray())
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        verboseLog("WSProtoProcessor process")

        val symbols = resolver
            .getSymbolsWithAnnotation(annotationFqName)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (symbols.isEmpty()) return emptyList()

        // KSP may call `process` multiple times. Symbols that still reference
        // unresolved types must be returned so they can be revisited in a later
        // round after other processors generate their dependencies.
        val (validSymbols, deferredSymbols) = symbols.partition { it.isProcessable() }
        validSymbols.forEach { it.accept(ProtoVisitor(), Unit) }
        return deferredSymbols
    }

    /**
     * Returns `true` only when the class and all of its properties can be read
     * safely in the current KSP round.
     */
    private fun KSClassDeclaration.isProcessable(): Boolean {
        if (!validate()) return false
        return getAllProperties().all { property ->
            property.validate() && !property.type.resolve().isError
        }
    }

    /**
     * Visitor responsible for turning a single Kotlin class into a `.proto`
     * schema file.
     */
    inner class ProtoVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            verboseLog("WSProtoProcessor visitClassDeclaration")

            // The annotation contract is intentionally narrow: only concrete
            // classes are supported as schema sources.
            if (classDeclaration.classKind != ClassKind.CLASS) {
                logger.error("Only class can be annotated with @WSProto", classDeclaration)
                return
            }

            // This processor uses only one annotation, so we can safely extract
            // the first matching instance and read its `name` argument.
            val annotation = classDeclaration.annotations.first {
                it.shortName.asString() == "WSProto"
            }

            val fileName = annotation.arguments.first {
                it.name?.asString() == "name"
            }.value as? String

            if (fileName == null) {
                logger.error("Invalid @WSProto annotation", annotation)
                return
            }

            if (!fileName.matches(VALID_PROTO_NAME_PATTERN)) {
                logger.error(
                    "@WSProto name must match pattern [a-z][a-z0-9_]* but was '$fileName'",
                    annotation,
                )
                return
            }

            if (!generatedNames.add(fileName)) {
                logger.error(
                    "Duplicate @WSProto name '$fileName' — each name must be unique",
                    annotation,
                )
                return
            }

            // Protobuf message names are conventionally PascalCase even when the
            // source file name is snake_case.
            val pascalCaseName = fileName.split('_')
                .joinToString("") { word ->
                    word.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }

            // The generated message name must differ from the original class name
            // so users can keep their Kotlin model and generated Protobuf class in
            // the same package without name collisions.
            if (pascalCaseName == classDeclaration.simpleName.asString()) {
                logger.error("Class Name must be different from file name")
                return
            }

            val containingFile = classDeclaration.containingFile
            if (containingFile == null) {
                logger.error("Unable to resolve containing file for '${classDeclaration.simpleName.asString()}'.")
                return
            }

            // Build everything in memory first. This avoids leaving a partially
            // written `.proto` file behind if an exception happens while mapping
            // one of the properties.
            val propertyDefinitions = classDeclaration.getAllProperties()
                .filter { it.validate() }
                .mapIndexed { index, property ->
                    buildFieldDefinition(property, index + 1)
                }

            val protoContent = buildString {
                append("syntax = \"proto3\";\n\n")
                append("option java_package = \"${processorOptions.protoJavaPackage}\";\n")
                append("option java_multiple_files = true;\n\n")
                append("message $pascalCaseName {\n")
                propertyDefinitions.forEach(::append)
                append("}")
            }

            codeGenerator.createNewFile(
                dependencies = Dependencies(false, containingFile),
                packageName = processorOptions.protoPackagePath,
                fileName = fileName,
                extensionName = "proto",
            ).use { outputFile ->
                outputFile += protoContent
            }
        }
    }

    /**
     * Converts one Kotlin property into a Protobuf field definition.
     *
     * Example:
     * `val userName: String` -> `string user_name = 1;`
     */
    private fun buildFieldDefinition(
        property: KSPropertyDeclaration,
        index: Int,
    ): String {
        verboseLog("WSProtoProcessor visitPropertyDeclaration")

        val ksType = property.type.resolve()
        val argType = getProtoTypeName(ksType)
        val argName = toSnakeCase(property.simpleName.asString())
        return "    $argType $argName = $index;\n"
    }

    /**
     * Converts a camelCase name to snake_case, handling consecutive uppercase
     * letters (acronyms) correctly.
     *
     * Examples:
     * - `userName` -> `user_name`
     * - `userURL` -> `user_url`
     * - `isHTTPSEnabled` -> `is_https_enabled`
     */
    private fun toSnakeCase(name: String): String = buildString {
        name.forEachIndexed { i, ch ->
            if (ch.isUpperCase()) {
                val prevIsLower = i > 0 && name[i - 1].isLowerCase()
                val nextIsLower = i + 1 < name.length && name[i + 1].isLowerCase()
                if (prevIsLower || (i > 0 && nextIsLower)) {
                    append('_')
                }
            }
            append(ch.lowercase())
        }
    }

    /**
     * Maps supported Kotlin types to their Protobuf equivalents.
     *
     * Unsupported types fail fast because generating a wrong schema would be
     * harder to debug than stopping compilation with a clear message.
     */
    private fun getProtoTypeName(ksType: KSType): String {
        val kotlinType = ksType.declaration.simpleName.asString()
        return when (kotlinType) {
            "Int", "Short", "Byte" -> "int32"
            "Long" -> "int64"
            "Float" -> "float"
            "Double" -> "double"
            "Boolean" -> "bool"
            "String" -> "string"
            "ByteArray" -> "bytes"
            "List", "Array" -> {
                resolveRepeatedType(ksType, kotlinType)
            }

            "Set" -> {
                logger.warn("Set<T> is mapped to 'repeated' which allows duplicates in proto3")
                resolveRepeatedType(ksType, kotlinType)
            }

            "Map" -> {
                val args = ksType.arguments
                val keyType = args[0].type?.resolve()
                val valueType = args[1].type?.resolve()
                if (keyType == null || valueType == null) {
                    logger.error("Unable to resolve Map type arguments")
                    throw IllegalArgumentException("Unable to resolve Map type arguments")
                }
                val keyTypeName = keyType.declaration.simpleName.asString()
                if (keyTypeName !in VALID_MAP_KEY_TYPES) {
                    logger.error("Proto3 map key must be an integral or string type, but was '$keyTypeName'")
                    throw IllegalArgumentException("Proto3 map key must be an integral or string type, but was '$keyTypeName'")
                }
                val valueTypeName = valueType.declaration.simpleName.asString()
                if (valueTypeName in COLLECTION_TYPE_NAMES || valueTypeName == "Map") {
                    logger.error("Proto3 map value cannot be a collection or map type")
                    throw IllegalArgumentException("Proto3 map value cannot be a collection or map type")
                }
                "map<${getProtoTypeName(keyType)}, ${getProtoTypeName(valueType)}>"
            }

            else -> {
                logger.error("Unsupported type: $kotlinType")
                throw IllegalArgumentException("Unsupported type: $kotlinType")
            }
        }
    }

    /**
     * Resolves the element type for a collection and produces the `repeated`
     * proto field prefix. Rejects nested collections.
     */
    private fun resolveRepeatedType(ksType: KSType, kotlinType: String): String = ksType.arguments.first().type?.resolve()?.let { type ->
        val elementType = type.declaration.simpleName.asString()
        if (elementType in COLLECTION_TYPE_NAMES) {
            logger.error("Nested collections are not supported in proto3 (found $kotlinType<$elementType<...>>)")
            throw IllegalArgumentException("Nested collections are not supported in proto3")
        }
        "repeated ${getProtoTypeName(type)}"
    } ?: run {
        logger.error("Unsupported type: $kotlinType")
        throw IllegalArgumentException("Unsupported type: $kotlinType")
    }

    override fun finish() {
        verboseLog("WSProtoProcessor finish")
        super.finish()
    }

    override fun onError() {
        // This callback is useful when debugging processor-level failures that
        // happened before a more specific error message reached the user.
        logger.error("WSProtoProcessor onError")
        super.onError()
    }
}
