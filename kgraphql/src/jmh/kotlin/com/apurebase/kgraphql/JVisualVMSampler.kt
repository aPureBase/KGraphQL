package com.apurebase.kgraphql


val schema = BenchmarkSchema.create { }

fun main(vararg args: String) {
    while (true) {
        println(schema.execute("{one{name, quantity, active}}"))
        println(schema.execute("{two(name : \"FELLA\"){range{start, endInclusive}}}"))
        println(schema.execute("{three{id}}"))
        Thread.sleep(10)
    }
}