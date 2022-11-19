package com.apurebase.kgraphql.integration

import com.apurebase.kgraphql.*
import org.amshove.kluent.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class MutationTest : BaseSchemaTest() {

    val testActor = Actor("Michael Caine", 72)

    @Test
    fun `simple mutation multiple fields`() {
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name, age}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf("name" to testActor.name, "age" to testActor.age))
        )
    }

    @Test
    fun `simple mutation single field`() {
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf<String, Any>("name" to testActor.name))
        )
    }

    @Test
    fun `simple mutation single field 2`() {
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf<String, Any>("age" to testActor.age))
        )
    }

    @Test
    fun `invalid mutation name`() {
        invoking {
            execute("mutation {createBanana(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        } shouldThrow GraphQLError::class withMessage "Property createBanana on Mutation does not exist"
    }

    @Test
    fun `invalid argument type`() {
        invoking {
            execute("mutation {createActor(name: \"${testActor.name}\", age: \"fwfwf\"){age}}")
        } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "Cannot coerce \"fwfwf\" to numeric constant"
        }

    }

    @Test
    fun `invalid arguments number`() {
        invoking {
            execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}, bananan: \"fwfwf\"){age}}")
        } shouldThrow ValidationException::class with {
            message shouldBeEqualTo "createActor does support arguments [name, age]. Found arguments [name, age, bananan]"
        }
    }

    @Test
    fun `mutation with alias`() {
        val map = execute("mutation {caine : createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/caine"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun `mutation with field alias`() {
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){howOld: age}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActor"),
            equalTo(mapOf<String, Any>("howOld" to testActor.age))
        )
    }

    @Test
    fun `simple mutation with aliased input type`() {
        val map = execute(
            "mutation(\$newActor: ActorInput!) { createActorWithAliasedInputType(newActor: \$newActor) {name}}",
            variables = "{\"newActor\": {\"name\": \"${testActor.name}\", \"age\": ${testActor.age}}}"
        )
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Any>>("data/createActorWithAliasedInputType"),
            equalTo(mapOf<String, Any>("name" to testActor.name))
        )
    }
}
