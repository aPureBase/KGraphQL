package com.apurebase.kgraphql.schema.model.ast

data class DirectiveNode(
    override val loc: Location?,
    val name: NameNode,
    val arguments: List<ArgumentNode>?,
) : ASTNode()
