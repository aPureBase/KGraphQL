package com.apurebase.kgraphql.schema.model.ast

import com.apurebase.kgraphql.schema.model.ast.ValueNode.StringValueNode

data class FieldDefinitionNode(
    override val loc: Location?,
    val name: NameNode,
    val description: StringValueNode?,
    val arguments: List<InputValueDefinitionNode>?,
    val type: TypeNode,
    val directives: List<DirectiveNode>?,
) : ASTNode()
