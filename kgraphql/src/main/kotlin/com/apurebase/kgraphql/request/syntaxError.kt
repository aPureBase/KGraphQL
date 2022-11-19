package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.schema.model.ast.Source

internal fun syntaxError(
    source: Source,
    position: Int,
    description: String,
) = GraphQLError(
    message = "Syntax Error: $description",
    nodes = null,
    source = source,
    positions = listOf(position)
)
