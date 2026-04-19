package com.wonseok.wspb.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Entry point that lets KSP create a [WSProtoProcessor] for each compilation.
 *
 * KSP instantiates the provider through Java's service loader mechanism and
 * passes a fresh [SymbolProcessorEnvironment] for the current compilation.
 */
class WSProtoProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = WSProtoProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
        options = environment.options,
    )
}
