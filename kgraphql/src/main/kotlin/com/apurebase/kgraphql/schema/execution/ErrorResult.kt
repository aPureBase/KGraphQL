package com.apurebase.kgraphql.schema.execution

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.schema.model.ast.SelectionNode

data class ErrorResult(
    val message: String = "",
    val path: List<Any> = listOf(),
    val locations: List<Pair<Int, Int>> = listOf(),
    val extensions: Map<String, Any>? = mapOf()
)

interface ErrorFunction {
    fun errorMessageFunction(node: Execution.Node, e: GraphQLError): String
    fun errorPathFunction(node: Execution.Node, e: GraphQLError): List<Any>
    fun errorLocationsFunction(node: Execution.Node, e: GraphQLError): List<Pair<Int, Int>>
    fun errorExtensionsFunction(node: Execution.Node, e: GraphQLError): Map<String, Any>?
}

class DefaultErrorFunction : ErrorFunction {

    override fun errorMessageFunction(node: Execution.Node, e: GraphQLError): String {
        return if (e.originalError?.message.isNullOrBlank().not()) {
            e.originalError?.message.orEmpty()
        } else if (e.cause?.message.isNullOrBlank().not()) {
            e.cause?.message.orEmpty()
        } else {
            ""
        }
    }

    override fun errorPathFunction(node: Execution.Node, e: GraphQLError): List<Any> {
        return e.nodes?.flatMap { n ->
            when(n) {
                is SelectionNode -> {
                    n.fullPath.split("\\.").map {
                        try {
                            it.toInt()
                        } catch (e: NumberFormatException) {
                            it
                        }
                    }
                }
                else -> {
                    listOf(n.loc?.source?.name.orEmpty())
                }
            }
        }.orEmpty()
    }

    override fun errorLocationsFunction(node: Execution.Node, e: GraphQLError): List<Pair<Int, Int>> {
        return e.locations?.map {
            Pair(it.line, it.column)
        }.orEmpty()
    }

    override fun errorExtensionsFunction(node: Execution.Node, e: GraphQLError): Map<String, Any>? {
        return null
    }
}
