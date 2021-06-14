package com.apurebase.kgraphql.schema.dsl

import com.apurebase.kgraphql.configuration.PluginConfiguration
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import com.apurebase.kgraphql.schema.execution.DefaultErrorFunction
import com.apurebase.kgraphql.schema.execution.ErrorFunction
import com.apurebase.kgraphql.schema.execution.Executor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

open class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var documentParserCacheMaximumSize: Long = 1000L
    var acceptSingleValueAsArray: Boolean = true
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
    var wrapErrors: Boolean = true
    var executor: Executor = Executor.Parallel
    var timeout: Long? = null
    var errorFunction: ErrorFunction = DefaultErrorFunction()

    private val plugins: MutableMap<KClass<*>, Any> = mutableMapOf()

    fun install(plugin: PluginConfiguration) {
        val kClass = plugin::class
        require(plugins[kClass] == null)
        plugins[kClass] = plugin
    }


    internal fun update(block: SchemaConfigurationDSL.() -> Unit) = block()
    internal fun build(): SchemaConfiguration {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, acceptSingleValueAsArray)
        return SchemaConfiguration(
            useCachingDocumentParser,
            documentParserCacheMaximumSize,
            objectMapper,
            useDefaultPrettyPrinter,
            coroutineDispatcher,
            wrapErrors,
            executor,
            timeout,
            plugins,
            errorFunction
        )
    }

}
