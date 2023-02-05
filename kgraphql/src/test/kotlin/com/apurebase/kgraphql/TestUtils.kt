package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.DefaultSchema
import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.CoreMatchers
import org.hamcrest.FeatureMatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.instanceOf
import java.io.File

val objectMapper = jacksonObjectMapper()

fun deserialize(json: String) : Map<*,*> {
    return objectMapper.readValue(json, HashMap::class.java)
}

fun String.deserialize(): java.util.HashMap<*, *> = objectMapper.readValue(this, HashMap::class.java)

fun getMap(map : Map<*,*>, key : String) : Map<*,*>{
    return map[key] as Map<*,*>
}

@Suppress("UNCHECKED_CAST")
fun <T> Map<*, *>.extract(path: String) : T {
    val tokens = path.trim().split('/').filter(String::isNotBlank)
    try {
        return tokens.fold(this as Any?) { workingMap, token ->
            if(token.contains('[')){
                if (!(workingMap as Map<*, *>).containsKey(token.substringBefore('['))) throw IllegalArgumentException()
                val list = workingMap[token.substringBefore('[')]
                val index = token.substring(token.indexOf('[')+1, token.length -1).toInt()
                (list as List<*>)[index]
            } else {
                if (!(workingMap as Map<*, *>).containsKey(token)) throw IllegalArgumentException()
                workingMap[token]
            }
        } as T
    } catch (e : Exception){
        throw IllegalArgumentException("Path: $path does not exist in map: $this", e)
    }
}

fun defaultSchema(block: SchemaBuilder.() -> Unit): DefaultSchema {
    return SchemaBuilder().apply(block).build() as DefaultSchema
}

fun assertNoErrors(map : Map<*,*>) {
    if(map["errors"] != null) throw AssertionError("Errors encountered: ${map["errors"]}")
    if(map["data"] == null) throw AssertionError("Data is null")
}

fun assertError(map : Map<*,*>, vararg messageElements : String) {
    val errorMessage = map.extract<String>("errors/message")
    assertThat(errorMessage, CoreMatchers.notNullValue())

    messageElements
        .filterNot { errorMessage.contains(it) }
        .forEach { throw AssertionError("Expected error message to contain $it, but was: $errorMessage") }
}

inline fun <reified T: Exception> expect(message: String? = null, block: () -> Unit){
    try {
        block()
        throw AssertionError("No exception caught")
    } catch (e : Exception){
        assertThat(e, instanceOf(T::class.java))
        if(message != null){
            assertThat(e, ExceptionMessageMatcher(message))
        }
    }
}

fun executeEqualQueries(schema: Schema, expected: Map<*,*>, vararg queries : String) {
    queries.map { request ->
        schema.executeBlocking(request).deserialize()
    }.forEach { map ->
        map shouldBeEqualTo expected
    }
}

class ExceptionMessageMatcher(message: String?)
    : FeatureMatcher<Exception, String>(Matchers.containsString(message), "exception message is", "exception message"){

    override fun featureValueOf(actual: Exception?): String? = actual?.message
}

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun Any.getResourceAsFile(name: String): File = this::class.java.classLoader.getResource(name).toURI().let(::File)

object ResourceFiles {
    val kitchenSinkQuery = getResourceAsFile("kitchen-sink.graphql").readText()
}


const val d = '$'
