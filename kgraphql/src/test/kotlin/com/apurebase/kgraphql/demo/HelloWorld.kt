package com.apurebase.kgraphql.demo

import com.apurebase.kgraphql.KGraphQL

fun main() {
    val schema = KGraphQL.schema {
        query("hello") {
            resolver { name: String -> "Hello, $name" }
        }
    }

    //prints '{"data":{"hello":"Hello, Ted Mosby"}}'
    println(schema.executeBlocking("{hello(name : \"Ted Mosby\")}"))
}
