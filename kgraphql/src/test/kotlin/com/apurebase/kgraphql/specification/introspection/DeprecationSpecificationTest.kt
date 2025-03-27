package com.apurebase.kgraphql.specification.introspection

import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class DeprecationSpecificationTest {

    @Test
    fun `queries may be documented`(){
        val expected = "sample query"
        val schema = defaultSchema {
            query("sample"){
                deprecate(expected)
                resolver<String> {"SAMPLE"}
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{queryType{fields(includeDeprecated: true){name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/queryType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/queryType/fields[0]/isDeprecated"), equalTo(true))
    }

    @Test
    fun `mutations may be documented`(){
        val expected = "sample mutation"
        val schema = defaultSchema {
            mutation("sample"){
                deprecate(expected)
                resolver<String> {"SAMPLE"}
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{mutationType{fields(includeDeprecated: true){name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/isDeprecated"), equalTo(true))
    }

    data class Sample(val content : String)

    @Test
    fun `kotlin field may be documented`(){
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            type<Sample>{
                Sample::content.configure {
                    deprecate(expected)
                }
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields(includeDeprecated: true){isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[0]/deprecationReason/"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[0]/isDeprecated/"), equalTo(true))
    }

    @Test
    fun `extension field may be documented`(){
        val expected = "sample type"
        val expectedDescription = "add operation"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            type<Sample>{
                property<String>("add"){
                    description = expectedDescription
                    deprecate(expected)
                    resolver{ (content) -> content.uppercase() }
                }
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields(includeDeprecated: true){name, description, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[1]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[1]/description"), equalTo(expectedDescription))
        assertThat(response.extract("data/__type/fields[1]/isDeprecated"), equalTo(true))
    }

    enum class SampleEnum { ONE, TWO, THREE }

    @Test
    fun `enum value may be deprecated`(){
        val expected = "some enum value"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            enum<SampleEnum> {
                value(SampleEnum.ONE){
                    deprecate(expected)
                }
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"SampleEnum\"){enumValues(includeDeprecated: true){name, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/enumValues[0]/name"), equalTo(SampleEnum.ONE.name))
        assertThat(response.extract("data/__type/enumValues[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/enumValues[0]/isDeprecated"), equalTo(true))
    }

    data class Documented(val id: Int)

    @Test
    fun `type may be documented`(){
        val expected = "very documented type"
        val schema = defaultSchema {
            query("documented"){
                resolver { -> Documented(1) }
            }

            type<Documented> {
                description = "very documented type"
            }
        }

        val response = deserialize(schema.executeBlocking("query { __type(name: \"Documented\") {  name, kind, description } }"))
        assertThat(response.extract("data/__type/description"), equalTo(expected))
    }
}
