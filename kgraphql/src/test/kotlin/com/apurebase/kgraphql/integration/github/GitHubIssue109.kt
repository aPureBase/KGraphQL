package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.KGraphQL
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class GitHubIssue109 {

    data class Wrapper(
        val items: List<QualificationItem>,
    )

    sealed class QualificationItem {
        data class Qual1(
            val id: String,
        ) : QualificationItem()
    }

    @Test
    fun `Confusion with a list of union type`() {
        val schema = KGraphQL.schema {
            unionType<QualificationItem>()

            query("foo") {
                resolver { ->
                    Wrapper(listOf(QualificationItem.Qual1("12345")))
                }
            }
        }

        val result = jacksonObjectMapper().readValue<Result>(
            schema.executeBlocking(
                """
                query IntrospectionQuery {
                    __schema {
                        types {
                            name
                            fields(includeDeprecated: true) {
                                name
                                type {
                                    name
                                }
                            }
                            possibleTypes {
                                name
                            }
                        }
                    }
                }
            """
            )
        )

        result.data.__schema.types.shouldContain(
            Type(
                name = "QualificationItem",
                possibleTypes = listOf(
                    FieldType("Qual1")
                )
            )
        )
        result.data.__schema.types.shouldContain(
            Type(
                name = "Qual1",
                fields = listOf(
                    Field(name = "id", type = FieldType())
                )
            )
        )
    }

    data class Result(val data: Data)
    data class Data(val __schema: Schema)
    data class Schema(val types: List<Type>)
    data class Type(
        val name: String?,
        val fields: List<Field>? = null,
        val possibleTypes: List<FieldType>? = null,
    )

    data class Field(
        val name: String?,
        val type: FieldType,
    )

    data class FieldType(val name: String? = null)
}
