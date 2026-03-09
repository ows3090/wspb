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
        symbols.forEach { it.accept(ProtoVisitor(resolver), Unit) }
        return symbols.filterNot { it.validate() }.toList()
    }

    inner class ProtoVisitor(
        private val resolver: Resolver,
    ) : KSVisitorVoid() {
        var file: OutputStream? = null

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

            file = codeGenerator.createNewFile(
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = processorOptions.protoPackagePath,
                fileName = fileName,
                extensionName = "proto",
            )

            file!! += "syntax = \"proto3\";\n\n"
            file!! += "option java_package = \"${processorOptions.protoJavaPackage}\";\n"
            file!! += "option java_multiple_files = true;\n\n"
            file!! += "message $pascalCaseName {\n"

            val properties = classDeclaration.getAllProperties().filter { it.validate() }
            if (properties.iterator().hasNext()) {
                properties.forEachIndexed { index, property ->
                    visitPropertyDeclaration(property, Unit)
                    file!! += "${index + 1};\n"
                }
            }

            file!! += "}"
            file?.close()
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            verboseLog("WSProtoProcessor visitPropertyDeclaration")

            val ksType = property.type.resolve()
            val argType = getProtoTypeName(ksType)
            var argName = ""
            property.simpleName.asString().forEachIndexed { index, ch ->
                if (ch.isUpperCase() && index > 0) {
                    argName += "_"
                }
                argName += ch.lowercase()
            }
            file!! += "    $argType $argName = "
        }
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
