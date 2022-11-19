package com.apurebase.kgraphql.schema.dsl

import com.apurebase.kgraphql.Scenario
import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.extract
import com.apurebase.kgraphql.schema.SchemaBuilderTest
import com.apurebase.kgraphql.schema.dsl.types.TypeDSL
import com.apurebase.kgraphql.schema.execution.Executor
import nidomiro.kdataloader.ExecutionResult
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class DataLoaderPropertyDSLTest {

    @Test
    fun `prepare() should support multiple arguments`() {
        val schema = defaultSchema {
            configure {
                executor = Executor.DataLoaderPrepared
            }
            query("parent") {
                resolver { -> Parent() }
            }
            type<Parent> {
                dataProperty<String, Parent>("child") {
                    prepare { _, a: String, b: Int, c: String, d: Int, e: String, f: Int, g: String, h: Int ->
                        "$a $b $c $d $e $f $g $h"
                    }
                    loader { keys ->
                        keys.map { ExecutionResult.Success(Parent(it)) }
                    }
                }
            }
        }
        val results = schema.executeBlocking(
            """
            {
                parent {
                    child(a: "A", b: 1, c: "C", d: 2, e: "E", f: 3, g: "G", h: 4) {
                        data
                    }
                }
            }
            """.trimIndent()
        ).deserialize()
        results.extract<String>("data/parent/child/data") shouldBeEqualTo "A 1 C 2 E 3 G 4"
    }

    class Parent(val data: String = "")

    inline fun <T : Any, reified P : Any> TypeDSL<T>.createGenericDataProperty(
        returnType: KType,
        crossinline resolver: () -> P,
    ) {
        dataProperty<T, P>("data") {
            prepare { it }
            loader { it.map { ExecutionResult.Success(resolver()) } }
            setReturnType(returnType)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `specifying return type explicitly allows generic data property creation`() {
        val schema = defaultSchema {
            configure {
                executor = Executor.DataLoaderPrepared
            }
            type<Scenario> {
                createGenericDataProperty(typeOf<SchemaBuilderTest.InputOne>()) { SchemaBuilderTest.InputOne("generic") }
            }
        }

        MatcherAssert.assertThat(schema.typeByKClass(SchemaBuilderTest.InputOne::class), CoreMatchers.notNullValue())
    }

    data class Prop<T>(val resultType: KType, val resolver: () -> T)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `creation of data properties from a list`() {

        val props = listOf(Prop(typeOf<Int>()) { 0 }, Prop(typeOf<String>()) { "test" })

        val schema = defaultSchema {
            configure {
                executor = Executor.DataLoaderPrepared
            }
            type<Scenario> {
                props.forEach { prop ->
                    createGenericDataProperty(prop.resultType, prop.resolver)
                }
            }
        }

        MatcherAssert.assertThat(schema.typeByKClass(Int::class), CoreMatchers.notNullValue())
        MatcherAssert.assertThat(schema.typeByKClass(String::class), CoreMatchers.notNullValue())
    }
}
