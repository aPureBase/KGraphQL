package com.apurebase.kgraphql.schema

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import com.apurebase.kgraphql.schema.execution.ExecutionOptions
import com.apurebase.kgraphql.schema.introspection.__Schema
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language

interface Schema : __Schema {
    val configuration: SchemaConfiguration

    suspend fun execute(
        @Language("graphql") request: String,
        variables: String? = null,
        context: Context = Context(emptyMap()),
        options: ExecutionOptions = ExecutionOptions(),
    ): String

    fun executeBlocking(
        @Language("graphql") request: String,
        variables: String? = null,
        context: Context = Context(emptyMap()),
        options: ExecutionOptions = ExecutionOptions(),
    ) = runBlocking { execute(request, variables, context, options) }
}
