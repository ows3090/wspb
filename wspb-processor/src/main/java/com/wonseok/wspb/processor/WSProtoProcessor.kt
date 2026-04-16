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

class WSProtoProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    options: Map<String, String>,
) : SymbolProcessor {
    private val processorOptions = ProcessorOptions.from(options, logger)
    private val annotationFqName = WSProto::class.qualifiedName ?: FALLBACK_ANNOTATION_FQ_NAME

    companion object {
        private const val FALLBACK_ANNOTATION_FQ_NAME = "com.wonseok.wspb.annotation.WSProto"
    }

    private fun verboseLog(message: String) {
        if (processorOptions.verbose) {
            logger.warn(message)
        }
    }

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
        val (validSymbols, deferredSymbols) = symbols.partition { it.isProcessable() }
        validSymbols.forEach { it.accept(ProtoVisitor(), Unit) }
        return deferredSymbols
    }

    private fun KSClassDeclaration.isProcessable(): Boolean {
        if (!validate()) return false
        return getAllProperties().all { property ->
            property.validate() && !property.type.resolve().isError
        }
    }

    inner class ProtoVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            verboseLog("WSProtoProcessor visitClassDeclaration")

            // Only class can be annotated with @WSProto
            if (classDeclaration.classKind != ClassKind.CLASS) {
                logger.error("Only class can be annotated with @WSProto", classDeclaration)
                return
            }

            // Find @WSProto annotation
            val annotation = classDeclaration.annotations.first {
                it.shortName.asString() == "WSProto"
            }

            // Find file name argument
            val fileName = annotation.arguments.first {
                it.name?.asString() == "name"
            }.value as? String

            if (fileName == null) {
                logger.error("Invalid @WSProto annotation", annotation)
                return
            }

            val pascalCaseName = fileName.split('_')
                .joinToString("") { word ->
                    word.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }

            if (pascalCaseName == classDeclaration.simpleName.asString()) {
                logger.error("Class Name must be different from file name")
                return
            }

            val containingFile = classDeclaration.containingFile
            if (containingFile == null) {
                logger.error("Unable to resolve containing file for '${classDeclaration.simpleName.asString()}'.")
                return
            }

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

    private fun buildFieldDefinition(
        property: KSPropertyDeclaration,
        index: Int,
    ): String {
        verboseLog("WSProtoProcessor visitPropertyDeclaration")

        val ksType = property.type.resolve()
        val argType = getProtoTypeName(ksType)
        var argName = ""
        property.simpleName.asString().forEachIndexed { charIndex, ch ->
            if (ch.isUpperCase() && charIndex > 0) {
                argName += "_"
            }
            argName += ch.lowercase()
        }
        return "    $argType $argName = $index;\n"
    }

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
        logger.error("WSProtoProcessor onError")
        super.onError()
    }
}
