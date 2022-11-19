package com.apurebase.kgraphql.specification.introspection

import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class DocumentationSpecificationTest {

    @Test
    fun `queries may be documented`() {
        val expected = "sample query"
        val schema = defaultSchema {
            query("sample") {
                description = expected
                resolver<String> { "SAMPLE" }
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{queryType{fields{name, description}}}}"))
        assertThat(response.extract("data/__schema/queryType/fields[0]/description"), equalTo(expected))
    }

    @Test
    fun `mutations may be documented`() {
        val expected = "sample mutation"
        val schema = defaultSchema {
            mutation("sample") {
                description = expected
                resolver<String> { "SAMPLE" }
            }
        }

        val response = deserialize(schema.executeBlocking("{__schema{mutationType{fields{name, description}}}}"))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/description"), equalTo(expected))
    }

    data class Sample(val content: String)

    @Test
    fun `object type may be documented`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<Sample> { Sample("SAMPLE") }
            }
            type<Sample> {
                description = expected
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){description}}"))
        assertThat(response.extract("data/__type/description/"), equalTo(expected))
    }

    @Test
    fun `input type may be documented`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            inputType<Sample> {
                description = expected
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){description}}"))
        assertThat(response.extract("data/__type/description/"), equalTo(expected))
    }

    @Test
    fun `kotlin field may be documented`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            type<Sample> {
                Sample::content.configure {
                    description = expected
                }
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields{description}}}"))
        assertThat(response.extract("data/__type/fields[0]/description/"), equalTo(expected))
    }

    @Test
    fun `extension field may be documented`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            type<Sample> {
                property<String>("add") {
                    description = expected
                    resolver { (content) -> content.toUpperCase() }
                }
            }
        }

        val response = deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields{name, description}}}"))
        assertThat(response.extract("data/__type/fields[1]/name/"), equalTo("add"))
        assertThat(response.extract("data/__type/fields[1]/description/"), equalTo(expected))
    }

    enum class SampleEnum { ONE, TWO, THREE }

    @Test
    fun `enum value may be documented`() {
        val expected = "some enum value"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            enum<SampleEnum> {
                value(SampleEnum.ONE) {
                    description = expected
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"SampleEnum\"){enumValues{name, description}}}"))
        assertThat(response.extract("data/__type/enumValues[0]/name"), equalTo(SampleEnum.ONE.name))
        assertThat(response.extract("data/__type/enumValues[0]/description"), equalTo(expected))
    }
}
