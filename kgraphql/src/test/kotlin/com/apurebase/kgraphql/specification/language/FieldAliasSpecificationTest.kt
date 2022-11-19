package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.Actor
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("2.7 Field Alias")
class FieldAliasSpecificationTest {

    val age = 232

    val actorName = "Boguś Linda"

    val schema = defaultSchema {
        query("actor") {
            resolver { -> Actor(actorName, age) }
        }

        type<Actor> {
            transformation(Actor::age) { age: Int, inMonths: Boolean? ->
                if (inMonths == true) age * 12 else age
            }
        }
    }

    @Test
    fun `can define response object field name`() {
        val map =
            deserialize(schema.executeBlocking("{actor{ageMonths: age(inMonths : true) ageYears: age(inMonths : false)}}"))
        assertThat(map.extract<Int>("data/actor/ageMonths"), equalTo(age * 12))
        assertThat(map.extract<Int>("data/actor/ageYears"), equalTo(age))
    }

    @Test
    fun `top level of a query can be given alias`() {
        val map = deserialize(schema.executeBlocking("{ bogus : actor{name}}"))
        assertThat(map.extract<String>("data/bogus/name"), equalTo(actorName))
    }
}
