package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.model.ast.ASTNode

class ValidationException(message: String, nodes: List<ASTNode>? = null) : GraphQLError(message, nodes = nodes)
