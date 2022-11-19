package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.integration.BaseSchemaTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Specification("2.10 Variables")
class VariablesSpecificationTest : BaseSchemaTest() {
    @Test
    fun `query with variables`() {
        val map = execute(
            query = "mutation(\$name: String!, \$age : Int!) {createActor(name: \$name, age: \$age){name, age}}",
            variables = "{\"name\":\"Boguś Linda\", \"age\": 22}"
        )
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `query with boolean variable`() {
        val map = execute(query = "query(\$big: Boolean!) {number(big: \$big)}", variables = "{\"big\": true}")
        assertNoErrors(map)
        assertThat(map.extract<Int>("data/number"), equalTo(10000))
    }

    @Test
    fun `query with boolean variable default value`() {
        val map = execute(query = "query(\$big: Boolean = true) {number(big: \$big)}")
        assertNoErrors(map)
        assertThat(map.extract<Int>("data/number"), equalTo(10000))
    }

    @Test
    fun `query with variables and string default value`() {
        val map = execute(
            query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!) {createActor(name: \$name, age: \$age){name, age}}",
            variables = "{\"age\": 22}"
        )
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    @Disabled("I don't think this should actually be supported?")
    fun `query with variables and default value pointing to another variable`() {
        val map = execute(
            query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int = \$defaultAge, \$defaultAge : Int!) " +
                    "{createActor(name: \$name, age: \$age){name, age}}",
            variables = "{\"defaultAge\": 22}"
        )
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `fragment with variable`() {
        val map = execute(
            query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!, \$big: Boolean!) {createActor(name: \$name, age: \$age){...Linda}}" +
                    "fragment Linda on Actor {picture(big: \$big)}",
            variables = "{\"age\": 22, \"big\": true}"
        )
        assertNoErrors(map)
        assertThat(
            map.extract<String>("data/createActor/picture"),
            equalTo("http://picture.server/pic/Boguś_Linda?big=true")
        )
    }

    @Test
    fun `fragment with missing variable`() {
        expect<IllegalArgumentException>("Variable '\$big' was not declared for this operation") {
            execute(
                query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!) {createActor(name: \$name, age: \$age){...Linda}}" +
                        "fragment Linda on Actor {picture(big: \$big)}",
                variables = "{\"age\": 22}"
            )
        }
    }

    @Test
    fun `Advanced variables`() {
        val d = "$"
        val request = """
            mutation MultipleCreate(
                ${d}name1: String!,
                ${d}age1: Int!,
                ${d}name2: String!,
                ${d}age2: Int!,
                ${d}input1: ActorInput!,
                ${d}input2: ActorInput!,
                ${d}agesName1: String!,
                ${d}ages1: [Int!]!,
                ${d}agesName2: String!,
                ${d}ages2: [Int!]!,
                ${d}agesInput1: ActorCalculateAgeInput!,
                ${d}agesInput2: ActorCalculateAgeInput!
            ) {
                createFirst: createActor(name: ${d}name1, age: ${d}age1) { name, age }
                createSecond: createActor(name: ${d}name2, age: ${d}age2) { name, age }
                inputFirst: createActorWithInput(input: ${d}input1) { name, age }
                inputSecond: createActorWithInput(input: ${d}input2) { name, age }
                agesFirst: createActorWithAges(name: ${d}agesName1, ages: ${d}ages1) { name, age }
                agesSecond: createActorWithAges(name: ${d}agesName2, ages: ${d}ages2) { name, age }
                inputAgesFirst: createActorWithAgesInput(input: ${d}agesInput1) { name, age }
                inputAgesSecond: createActorWithAgesInput(input: ${d}agesInput2) { name, age }
            }
        """.trimEnd()
        val variables = """
            {
                "name1": "Jógvan",
                "age1": 1,
                "name2": "Paweł",
                "age2": 2,
                "input1": {"name": "Olsen", "age": 3},
                "input2": {"name": "Gutkowski", "age": 4},
                "agesName1": "Someone",
                "ages1": [10,50],
                "agesName2": "Some other",
                "ages2": [5, 10],
                "agesInput1": {"name": "Jógvan Olsen", "ages": [3]},
                "agesInput2": {"name": "Paweł Gutkowski", "ages": [4,5,6,7]}
            }
        """.trimIndent()

        val result = execute(request, variables)


        assertNoErrors(result)
        assertThat(result.extract("data/createFirst/name"), equalTo("Jógvan"))
        assertThat(result.extract("data/createSecond/age"), equalTo(2))
        assertThat(result.extract("data/inputFirst/name"), equalTo("Olsen"))
        assertThat(result.extract("data/inputSecond/age"), equalTo(4))
        assertThat(result.extract("data/agesFirst/name"), equalTo("Someone"))
        assertThat(result.extract("data/agesSecond/age"), equalTo(15))
        assertThat(result.extract("data/inputAgesFirst/name"), equalTo("Jógvan Olsen"))
        assertThat(result.extract("data/inputAgesSecond/age"), equalTo(22))
    }

    @Test
    fun `required variable arrays`() {
        val d = "$"
        val request = """
            mutation MultipleCreate(
                ${d}agesName1: String!,
                ${d}ages1: [Int!]!,
                ${d}agesName2: String!,
                ${d}ages2: [Int!]!
            ) {
                agesFirst: createActorWithAges(name: ${d}agesName1, ages: ${d}ages1) { name, age }
                agesSecond: createActorWithAges(name: ${d}agesName2, ages: ${d}ages2) { name, age }
            }
        """.trimIndent()
        val variables = """
            {
                "agesName1": "Someone",
                "ages1": [10,50],
                "agesName2": "Some other",
                "ages2": [5, 10]
            }
        """.trimIndent()

        val result = execute(request, variables)


        assertNoErrors(result)
        assertThat(result.extract("data/agesFirst/name"), equalTo("Someone"))
        assertThat(result.extract("data/agesSecond/age"), equalTo(15))
    }
}
