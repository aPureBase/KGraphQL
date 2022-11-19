package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("3.1.3 Interfaces")
class InterfacesSpecificationTest {
    interface SimpleInterface {
        val exe: String
    }

    data class Simple(override val exe: String, val stuff: String) : SimpleInterface

    val schema = KGraphQL.schema {
        type<Simple>()
        query("simple") { resolver { -> Simple("EXE", "CMD") as SimpleInterface } }
    }

    @Test
    fun `Interfaces represent a list of named fields and their arguments`() {
        val map = deserialize(schema.executeBlocking("{simple{exe}}"))
        assertThat(map.extract("data/simple/exe"), equalTo("EXE"))
    }

    @Test
    fun `When querying for fields on an interface type, only those fields declared on the interface may be queried`() {
        invoking {
            schema.executeBlocking("{simple{exe, stuff}}")
        } shouldThrow GraphQLError::class withMessage "Property stuff on SimpleInterface does not exist"
    }

    @Test
    fun `Query for fields of interface implementation can be done only by fragments`() {
        val map = deserialize(schema.executeBlocking("{simple{exe ... on Simple { stuff }}}"))
        assertThat(map.extract("data/simple/stuff"), equalTo("CMD"))
    }
}
