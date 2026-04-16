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

    companion object {
        private const val FALLBACK_ANNOTATION_FQ_NAME = "com.wonseok.wspb.annotation.WSProto"
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
        var argName = ""
        property.simpleName.asString().forEachIndexed { charIndex, ch ->
            // Convert camelCase Kotlin property names into snake_case field names.
            if (ch.isUpperCase() && charIndex > 0) {
                argName += "_"
            }
            argName += ch.lowercase()
        }
        return "    $argType $argName = $index;\n"
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
            "List", "Set", "Array" -> {
                // Collection element types are resolved recursively so nested
                // primitive collections still produce valid repeated fields.
                ksType.arguments.first().type?.resolve()?.let { type ->
                    "repeated ${getProtoTypeName(type)}"
                } ?: run {
                    logger.error("Unsupported type: $kotlinType")
                    throw IllegalArgumentException("Unsupported type: $kotlinType")
                }
            }

            else -> {
                logger.error("Unsupported type: $kotlinType")
                throw IllegalArgumentException("Unsupported type: $kotlinType")
            }
        }
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
