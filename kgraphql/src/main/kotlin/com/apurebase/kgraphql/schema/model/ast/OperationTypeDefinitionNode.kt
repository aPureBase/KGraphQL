package com.apurebase.kgraphql.schema.model.ast

data class OperationTypeDefinitionNode(
    val operation: OperationTypeNode,
    val type: TypeNode.NamedTypeNode,
    override val loc: Location?,
) : ASTNode()
