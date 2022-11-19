package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.ResourceFiles.kitchenSinkQuery
import com.apurebase.kgraphql.d
import com.apurebase.kgraphql.schema.model.ast.*
import com.apurebase.kgraphql.schema.model.ast.DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode
import com.apurebase.kgraphql.schema.model.ast.OperationTypeNode.QUERY
import com.apurebase.kgraphql.schema.model.ast.SelectionNode.FieldNode
import com.apurebase.kgraphql.schema.model.ast.TokenKindEnum.EOF
import com.apurebase.kgraphql.schema.model.ast.TokenKindEnum.SOF
import com.apurebase.kgraphql.schema.model.ast.TypeNode.*
import com.apurebase.kgraphql.schema.model.ast.ValueNode.*
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class ParserTest {

    private fun parse(source: String, options: Parser.Options? = null) = try {
        Parser(source, options).parseDocument()
    } catch (e: GraphQLError) {
        println(e.prettyPrint())
        throw e
    }

    private fun parse(source: Source) = Parser(source).parseDocument()

    private fun parseValue(source: String): ValueNode {
        val parser = Parser(source)
        parser.expectToken(SOF)
        val value = parser.parseValueLiteral(false)
        parser.expectToken(EOF)
        return value
    }

    private fun parseType(source: String): TypeNode {
        val parser = Parser(source)
        parser.expectToken(SOF)
        val type = parser.parseTypeReference()
        parser.expectToken(EOF)
        return type
    }

    private fun shouldThrowSyntaxError(
        src: Source,
        block: GraphQLError.() -> Pair<Int, Int>?,
    ) = invoking { parse(src) } shouldThrow GraphQLError::class with {
        block()?.let {
            locations!!.size shouldBeEqualTo 1
            locations!!.first().run {
                line shouldBeEqualTo it.first
                column shouldBeEqualTo it.second
            }
        }
    }

    private fun shouldThrowSyntaxError(src: String, block: GraphQLError.() -> Pair<Int, Int>?) = shouldThrowSyntaxError(
        Source(src), block
    )

    @Test
    fun `parse provides useful errors`() {
        invoking { parse("{") } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "Syntax Error: Expected Name, found <EOF>."
            positions!!.size shouldBeEqualTo 1
            locations!!.size shouldBeEqualTo 1
            positions!!.first() shouldBeEqualTo 1
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }

            prettyPrint() shouldBeEqualTo """
                        |Syntax Error: Expected Name, found <EOF>.
                        |
                        |GraphQL request:1:2
                        |1 | {
                        |  |  ^
                    """.trimMargin()
        }

        shouldThrowSyntaxError(
            """
            |
            |      { ...MissingOn }
            |      fragment MissingOn Type
            |
        """.trimMargin()
        ) {
            message shouldBeEqualTo "Syntax Error: Expected \"on\", found Name \"Type\"."
            3 to 26
        }

        shouldThrowSyntaxError("{ field: {} }") {
            message shouldBeEqualTo "Syntax Error: Expected Name, found \"{\"."
            1 to 10
        }

        shouldThrowSyntaxError("notanoperation Foo { field }") {
            message shouldBeEqualTo "Syntax Error: Unexpected Name \"notanoperation\"."
            1 to 1
        }

        shouldThrowSyntaxError("...") {
            message shouldBeEqualTo "Syntax Error: Unexpected \"...\"."
            1 to 1
        }

        shouldThrowSyntaxError("{ \"\"") {
            message shouldBeEqualTo "Syntax Error: Expected Name, found String \"\"."
            1 to 3
        }
    }

    @Test
    fun `parse provides useful error when using source`() {
        shouldThrowSyntaxError(Source("query", "MyQuery.graphql")) {
            prettyPrint() shouldBeEqualTo """
                        |Syntax Error: Expected "{", found <EOF>.
                        |
                        |MyQuery.graphql:1:6
                        |1 | query
                        |  |      ^
                        """.trimMargin()
            null
        }
    }

    @Test
    fun `parses variable inline values`() {
        parse("{ field(complex: { a: { b: [ \$var ] } }) }")
    }

    @Test
    fun `parses constant default values`() {
        shouldThrowSyntaxError("query Foo(\$x: Complex = { a: { b: [ \$var ] } }) { field }") {
            message shouldBeEqualTo "Syntax Error: Unexpected \"\$\"."
            1 to 37
        }
    }

    @Test
    fun `parses variable definition directives`() {
        parse("query Foo(\$x: Boolean = false @bar) { field }")
    }

    @Test
    fun `does not accept fragments named 'on'`() {
        shouldThrowSyntaxError("fragment on on on { on }") {
            message shouldBeEqualTo "Syntax Error: Unexpected Name \"on\"."
            1 to 10
        }
    }

    @Test
    fun `does not accept fragments spread of 'on'`() {
        shouldThrowSyntaxError("{ ...on }") {
            message shouldBeEqualTo "Syntax Error: Expected Name, found \"}\"."
            1 to 9
        }
    }

    @Test
    fun `parses multi-byte characters`() {
        // Note: \u0A0A could be naively interpreted as two line-feed chars.
        parse(
            """
            |# This comment has a ${'\u0A0A'} multi-byte character.
            |{ field(arg: "Has a ${'\u0A0A'} multi-byte character.") }
        """.trimMargin()
        ).run {
            (definitions[0] as OperationDefinitionNode).selectionSet.run {
                (selections.first() as FieldNode).run {
                    (arguments!!.first().value as StringValueNode).run {
                        value shouldBeEqualTo "Has a \u0A0A multi-byte character."
                    }
                }
            }
        }
    }

    @Test
    fun `parses kitchen sink`() {
        parse(kitchenSinkQuery)
    }

    @Test
    fun `allows non-keywords anywhere a Name is allowed`() {
        val nonKeywords = listOf(
            "on",
            "fragment",
            "query",
            "mutation",
            "subscription",
            "true",
            "false"
        )

        for (keyword in nonKeywords) {
            val fragmentName = if (keyword != "on") keyword else "a"
            val document = """
                |query $keyword {
                |  ... $fragmentName
                |  ... on $keyword { field }
                |}
                |fragment $fragmentName on Type {
                |  $keyword($keyword: ${d}$keyword)
                |    @$keyword($keyword: $keyword)
                |}
            """.trimMargin()

            parse(document)
        }
    }

    @Test
    fun `parses anonymous mutation operations`() {
        parse(
            """
            |mutation {
            |  mutationField
            |}
        """.trimMargin()
        )
    }

    @Test
    fun `parses anonymous subscription operations`() {
        parse(
            """
            |subscription {
            |  subscriptionField
            |}
        """.trimMargin()
        )
    }

    @Test
    fun `parses named mutation operations`() {
        parse(
            """
            |mutation Foo {
            |  mutationField
            |}
        """.trimMargin()
        )
    }

    @Test
    fun `parses named subscription operations`() {
        parse(
            """
            |subscription Foo {
            |  subscriptionField
            |}
        """.trimMargin()
        )
    }

    @Test
    fun `creates ast`() {
        parse(
            """
            |{
            |  node(id: 4) {
            |    id,
            |    name
            |  }
            |}
            |
        """.trimMargin()
        ).run {
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 41
            }
            definitions.size shouldBeEqualTo 1
            (definitions.first() as OperationDefinitionNode).run {
                loc!!.run {
                    start shouldBeEqualTo 0
                    end shouldBeEqualTo 40
                }
                operation shouldBeEqualTo QUERY
                name shouldBeEqualTo null
                variableDefinitions!!.size shouldBeEqualTo 0
                directives!!.size shouldBeEqualTo 0
                selectionSet.run {
                    loc!!.run {
                        start shouldBeEqualTo 0
                        end shouldBeEqualTo 40
                    }
                    selections.size shouldBeEqualTo 1
                    (selections.first() as FieldNode).run {
                        loc!!.run {
                            start shouldBeEqualTo 4
                            end shouldBeEqualTo 38
                        }
                        alias shouldBeEqualTo null
                        name.loc!!.run {
                            start shouldBeEqualTo 4
                            end shouldBeEqualTo 8
                        }
                        name.value shouldBeEqualTo "node"
                        arguments!!.size shouldBeEqualTo 1
                        arguments!!.first().run {
                            name.value shouldBeEqualTo "id"
                            name.loc!!.run {
                                start shouldBeEqualTo 9
                                end shouldBeEqualTo 11
                            }
                            (value as NumberValueNode).run {
                                this shouldBeInstanceOf NumberValueNode::class
                                loc!!.run {
                                    start shouldBeEqualTo 13
                                    end shouldBeEqualTo 14
                                }
                                value shouldBeEqualTo 4
                            }
                            loc!!.run {
                                start shouldBeEqualTo 9
                                end shouldBeEqualTo 14
                            }
                        }

                        directives!!.size shouldBeEqualTo 0
                        selectionSet!!.run {
                            this shouldBeInstanceOf SelectionSetNode::class
                            loc!!.run {
                                start shouldBeEqualTo 16
                                end shouldBeEqualTo 38
                            }
                            selections.size shouldBeEqualTo 2
                            (selections.first() as FieldNode).run {
                                this shouldBeInstanceOf FieldNode::class
                                loc!!.run {
                                    start shouldBeEqualTo 22
                                    end shouldBeEqualTo 24
                                }
                                alias shouldBeEqualTo null
                                name.run {
                                    loc!!.run {
                                        start shouldBeEqualTo 22
                                        end shouldBeEqualTo 24
                                    }
                                    value shouldBeEqualTo "id"
                                }
                                arguments!!.size shouldBeEqualTo 0
                                directives!!.size shouldBeEqualTo 0
                                selectionSet shouldBeEqualTo null
                            }

                            (selections[1] as FieldNode).run {
                                this shouldBeInstanceOf FieldNode::class
                                loc!!.run {
                                    start shouldBeEqualTo 30
                                    end shouldBeEqualTo 34
                                }
                                alias shouldBeEqualTo null
                                name.run {
                                    loc!!.run {
                                        start shouldBeEqualTo 30
                                        end shouldBeEqualTo 34
                                        value shouldBeEqualTo "name"
                                    }
                                    arguments!!.size shouldBeEqualTo 0
                                    directives!!.size shouldBeEqualTo 0
                                    selectionSet shouldBeEqualTo null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `creates ast from nameless query without variables`() {
        parse(
            """
            |query {
            |  node {
            |    id
            |  }
            |}
            |
        """.trimMargin()
        ).run {
            this shouldBeInstanceOf DocumentNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 30
            }
            definitions.size shouldBeEqualTo 1
            (definitions.first() as OperationDefinitionNode).run {
                this shouldBeInstanceOf OperationDefinitionNode::class
                loc!!.run {
                    start shouldBeEqualTo 0
                    end shouldBeEqualTo 29
                }
                operation shouldBeEqualTo QUERY
                name shouldBeEqualTo null
                variableDefinitions!!.size shouldBeEqualTo 0
                directives!!.size shouldBeEqualTo 0
                selectionSet.run {
                    loc!!.run {
                        start shouldBeEqualTo 6
                        end shouldBeEqualTo 29
                    }
                    selections.size shouldBeEqualTo 1
                    (selections.first() as FieldNode).run {
                        this shouldBeInstanceOf FieldNode::class
                        loc!!.run {
                            start shouldBeEqualTo 10
                            end shouldBeEqualTo 27
                        }
                        alias shouldBeEqualTo null
                        name.run {
                            loc!!.run {
                                start shouldBeEqualTo 10
                                end shouldBeEqualTo 14
                            }
                            value shouldBeEqualTo "node"
                        }
                        arguments!!.size shouldBeEqualTo 0
                        directives!!.size shouldBeEqualTo 0
                        selectionSet!!.run {
                            loc!!.run {
                                start shouldBeEqualTo 15
                                end shouldBeEqualTo 27
                            }
                            selections.size shouldBeEqualTo 1
                            (selections.first() as FieldNode).run {
                                this shouldBeInstanceOf FieldNode::class
                                loc!!.run {
                                    start shouldBeEqualTo 21
                                    end shouldBeEqualTo 23
                                }
                                alias shouldBeEqualTo null
                                name.run {
                                    loc!!.run {
                                        start shouldBeEqualTo 21
                                        end shouldBeEqualTo 23
                                    }
                                    value shouldBeEqualTo "id"
                                }
                                arguments!!.size shouldBeEqualTo 0
                                directives!!.size shouldBeEqualTo 0
                                selectionSet shouldBeEqualTo null
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `allows parsing without source location information`() {
        val result = parse("{ id }", Parser.Options(noLocation = true))
        result.loc shouldBeEqualTo null
    }

    @Test
    fun `contains references to source`() {
        val source = Source("{ id }")
        val result = parse(source)

        result.loc!!.source shouldBeEqualTo source
    }

    @Test
    fun `contains references to start and end tokens`() {
        val result = parse("{ id }")

        result.loc!!.startToken.kind shouldBeEqualTo SOF
        result.loc!!.endToken.kind shouldBeEqualTo EOF
    }

    //================================================//
    ////////////////////////////////////////////////////
    //////////////////   parseValue   //////////////////
    ////////////////////////////////////////////////////
    //================================================//

    @Test
    fun `parses null value`() {
        parseValue("null").run {
            this shouldBeInstanceOf NullValueNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 4
            }
        }
    }

    @Test
    fun `parses list values`() {
        (parseValue("[123 \"abc\"]") as ListValueNode).run {
            this shouldBeInstanceOf ListValueNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 11
            }
            values.size shouldBeEqualTo 2
            (values[0] as NumberValueNode).run {
                this shouldBeInstanceOf NumberValueNode::class
                loc!!.run {
                    start shouldBeEqualTo 1
                    end shouldBeEqualTo 4
                }
                value shouldBeEqualTo 123
            }
            (values[1] as StringValueNode).run {
                this shouldBeInstanceOf StringValueNode::class
                loc!!.run {
                    start shouldBeEqualTo 5
                    end shouldBeEqualTo 10
                }
                value shouldBeEqualTo "abc"
                block shouldBeEqualTo false
            }
        }

    }

    @Test
    fun `parses block strings`() {
        (parseValue("[\"\"\"long\"\"\" \"short\"]") as ListValueNode).run {
            this shouldBeInstanceOf ListValueNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 20
            }
            values.size shouldBeEqualTo 2
            (values[0] as StringValueNode).run {
                this shouldBeInstanceOf StringValueNode::class
                loc!!.run {
                    start shouldBeEqualTo 1
                    end shouldBeEqualTo 11
                }
                value shouldBeEqualTo "long"
                block shouldBeEqualTo true
            }
            (values[1] as StringValueNode).run {
                this shouldBeInstanceOf StringValueNode::class
                loc!!.run {
                    start shouldBeEqualTo 12
                    end shouldBeEqualTo 19
                }
                value shouldBeEqualTo "short"
                block shouldBeEqualTo false
            }
        }
    }

    //===============================================//
    ///////////////////////////////////////////////////
    /////////////////   parseType   ///////////////////
    ///////////////////////////////////////////////////
    //===============================================//

    @Test
    fun `parses well known types`() {
        (parseType("String") as NamedTypeNode).run {
            this shouldBeInstanceOf NamedTypeNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 6
            }
            name.run {
                loc!!.run {
                    start shouldBeEqualTo 0
                    end shouldBeEqualTo 6
                }
                value shouldBeEqualTo "String"
            }
        }
    }

    @Test
    fun `parses custom types`() {
        (parseType("MyType") as NamedTypeNode).run {
            this shouldBeInstanceOf NamedTypeNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 6
            }
            name.run {
                loc!!.run {
                    start shouldBeEqualTo 0
                    end shouldBeEqualTo 6
                }
                value shouldBeEqualTo "MyType"
            }
        }
    }

    @Test
    fun `parses list types`() {
        (parseType("[MyType]") as ListTypeNode).run {
            this shouldBeInstanceOf ListTypeNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldEqualTo 8
            }
            (type as NamedTypeNode).run {
                this shouldBeInstanceOf NamedTypeNode::class
                loc!!.run {
                    start shouldBeEqualTo 1
                    end shouldBeEqualTo 7
                }
                name.run {
                    loc!!.run {
                        start shouldBeEqualTo 1
                        end shouldBeEqualTo 7
                    }
                    value shouldBeEqualTo "MyType"
                }
            }
        }
    }

    @Test
    fun `parses non-null types`() {
        (parseType("MyType!") as NonNullTypeNode).run {
            this shouldBeInstanceOf NonNullTypeNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 7
            }
            (type as NamedTypeNode).run {
                this shouldBeInstanceOf NamedTypeNode::class
                loc!!.run {
                    start shouldBeEqualTo 0
                    end shouldBeEqualTo 6
                }
                name.run {
                    loc!!.run {
                        start shouldBeEqualTo 0
                        end shouldBeEqualTo 6
                    }
                    value shouldBeEqualTo "MyType"
                }
            }
        }
    }

    @Test
    fun `parses nested types`() {
        (parseType("[MyType!]") as ListTypeNode).run {
            this shouldBeInstanceOf ListTypeNode::class
            loc!!.run {
                start shouldBeEqualTo 0
                end shouldBeEqualTo 9
            }
            (type as NonNullTypeNode).run {
                this shouldBeInstanceOf NonNullTypeNode::class
                loc!!.run {
                    start shouldBeEqualTo 1
                    end shouldBeEqualTo 8
                }
                (type as NamedTypeNode).run {
                    this shouldBeInstanceOf NamedTypeNode::class
                    loc!!.run {
                        start shouldBeEqualTo 1
                        end shouldBeEqualTo 7
                    }
                    name.run {
                        loc!!.run {
                            start shouldBeEqualTo 1
                            end shouldBeEqualTo 7
                        }
                        value shouldBeEqualTo "MyType"
                    }
                }
            }
        }
    }

}
