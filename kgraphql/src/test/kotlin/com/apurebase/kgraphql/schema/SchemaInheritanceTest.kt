package com.apurebase.kgraphql.schema

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.deserialize
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import java.util.*


class SchemaInheritanceTest {

    open class A(open var name: String = "", open var age: Int = 0) {
        var id: String = UUID.randomUUID().toString()
    }

    class B(name: String, age: Int, var pesel: String = "") : A(name, age)

    class C(override var name: String, override var age: Int, var pesel: String = "") : A(name, age)

    @Test
    fun `call to ignore property should cascade to subclasses`() {
        val name = "PELE"
        val age = 20

        val schema = KGraphQL.schema {

            type<A> { A::id.ignore() }

            query("b") { resolver { -> B(name, age) } }

            query("c") { resolver { -> C(name, age) } }
        }

        invoking {
            deserialize(schema.executeBlocking("{b{id, name, age}}"))
        } shouldThrow GraphQLError::class withMessage "Property id on B does not exist"

        invoking {
            deserialize(schema.executeBlocking("{c{id, name, age}}"))
        } shouldThrow GraphQLError::class withMessage "Property id on C does not exist"
    }

}
