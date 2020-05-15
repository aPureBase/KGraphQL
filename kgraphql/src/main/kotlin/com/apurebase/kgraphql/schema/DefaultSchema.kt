package com.apurebase.kgraphql.schema

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import com.apurebase.kgraphql.request.CachingDocumentParser
import com.apurebase.kgraphql.request.VariablesJson
import com.apurebase.kgraphql.schema.introspection.__Schema
import com.apurebase.kgraphql.request.Parser
import com.apurebase.kgraphql.schema.execution.*
import com.apurebase.kgraphql.schema.execution.Executor.*
import com.apurebase.kgraphql.schema.model.ast.*
import com.apurebase.kgraphql.schema.structure.LookupSchema
import com.apurebase.kgraphql.schema.structure.RequestInterpreter
import com.apurebase.kgraphql.schema.structure.SchemaModel
import com.apurebase.kgraphql.schema.structure.Type
import kotlinx.coroutines.coroutineScope
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

class DefaultSchema (
        override val configuration: SchemaConfiguration,
        internal val model : SchemaModel
) : Schema , __Schema by model, LookupSchema {

    companion object {
        val OPERATION_NAME_PARAM = NameNode("operationName", null)
    }

    private val defaultRequestExecutor: RequestExecutor = getExecutor(configuration.executor)

    private fun getExecutor(executor: Executor) = when (executor) {
        Parallel -> ParallelRequestExecutor(this)
        DataLoaderPrepared -> DataLoaderPreparedRequestExecutor(this)
    }

     private val requestInterpreter : RequestInterpreter = RequestInterpreter(model)

    private val cacheParser: CachingDocumentParser by lazy { CachingDocumentParser(configuration.documentParserCacheMaximumSize) }

    override suspend fun execute(request: String, variables: String?, context: Context, options: ExecutionOptions): String = coroutineScope {
        val parsedVariables = variables
            ?.let { VariablesJson.Defined(configuration.objectMapper, variables) }
            ?: VariablesJson.Empty()

        val document = Parser(request).parseDocument()

        val executor = options.executor?.let(this@DefaultSchema::getExecutor) ?: defaultRequestExecutor
        val plan = requestInterpreter.createExecutionPlan(document, parsedVariables)

        executor.suspendExecute(
            plan = plan,
            variables = parsedVariables,
            context = context.withFields(document.fields)
        )
    }

    override fun typeByKClass(kClass: KClass<*>): Type? = model.queryTypes[kClass]

    override fun typeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun inputTypeByKClass(kClass: KClass<*>): Type? = model.inputTypes[kClass]

    override fun inputTypeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun typeByName(name: String): Type? = model.queryTypesByName[name]

    override fun inputTypeByName(name: String): Type? = model.inputTypesByName[name]
}
