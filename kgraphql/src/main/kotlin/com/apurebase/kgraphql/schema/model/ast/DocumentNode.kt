package com.apurebase.kgraphql.schema.model.ast

import java.util.*

data class DocumentNode(
    val loc: Location?,
    val definitions: List<DefinitionNode>

) {
    val fields: List<String> by lazy {
        val q: Queue<SelectionSetNode> = LinkedList<SelectionSetNode>()
        val defs = this.definitions.mapNotNull { d: DefinitionNode ->
            when (d) {
                is DefinitionNode.ExecutableDefinitionNode ->
                    d
                else -> null
            }
        }
        defs.forEach { d ->
            q.add(d.selectionSet)
        }

        val fields = mutableListOf<NameNode>()
        while (!q.isEmpty()) {
            val curr = q.remove()

            for (sel in curr.selections) {
                when (sel) {
                    is SelectionNode.FieldNode -> {
                        if (sel.selectionSet != null) {
                            q.add(sel.selectionSet)
                        }
                        else {
                            fields.add(sel.aliasOrName)
                        }
                    }
                }
            }
        }

        fields.map {
            it.value
        }
    }
}
