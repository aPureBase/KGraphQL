package com.apurebase.kgraphql.schema.model.ast

import com.apurebase.kgraphql.schema.model.ast.ValueNode.VariableNode

data class VariableDefinitionNode(
    override val loc: Location?,
    val variable: VariableNode,
    val type: TypeNode,
    val defaultValue: ValueNode?,
    val directives: List<DirectiveNode>?,
) : ASTNode()
