package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder


data class ModelOne(val name: String, val quantity: Int = 1, val active: Boolean = true)

data class ModelTwo(val one: ModelOne, val range: IntRange)

data class ModelThree(val id: String, val twos: List<ModelTwo>)

object BenchmarkSchema {
    val ones = listOf(ModelOne("DUDE"), ModelOne("GUY"), ModelOne("PAL"), ModelOne("FELLA"))

    val oneResolver: suspend () -> List<ModelOne> = { ones }

    val twoResolver: suspend (name: String) -> ModelTwo? = { name ->
        ones.find { it.name == name }?.let { ModelTwo(it, it.quantity..12) }
    }

    val threeResolver: suspend () -> ModelThree = { ModelThree("", ones.map { ModelTwo(it, it.quantity..10) }) }

    object HasOneResolver {
        fun oneResolver(): List<ModelOne> {
            return ones
        }
    }

    fun create(block: SchemaBuilder<Unit>.() -> Unit): Schema = KGraphQL.schema {
        block()
        query("one") {
            resolver(oneResolver)
        }
        query("two") {
            resolver(twoResolver)
        }
        query("three") {
            resolver(threeResolver)
        }
        query("threeKF") {
            HasOneResolver::oneResolver.toResolver()
        }
    }
}