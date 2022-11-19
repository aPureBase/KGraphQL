package com.apurebase.kgraphql

import com.apurebase.kgraphql.BenchmarkSchema.oneResolver
import com.apurebase.kgraphql.BenchmarkSchema.threeResolver
import com.apurebase.kgraphql.BenchmarkSchema.twoResolver
import com.apurebase.kgraphql.schema.Schema
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Warmup(iterations = 15)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class SimpleExecutionOverheadBenchmark {

    @Param("true", "false")
    var withKGraphQL = true

    lateinit var schema: Schema

    lateinit var objectMapper: ObjectMapper

    @Setup
    fun setup() {
        if (withKGraphQL) {
            schema = BenchmarkSchema.create {}
        } else {
            objectMapper = jacksonObjectMapper()
        }
    }

    @Benchmark
    fun benchmark(): String {
        if (withKGraphQL) {
            return schema.executeBlocking("{one{name, quantity, active}, two(name : \"FELLA\"){range{start, endInclusive}}, three{id}}")
        } else {
            return runBlocking {
                ": ${objectMapper.writeValueAsString(oneResolver())} " +
                        ": ${objectMapper.writeValueAsString(twoResolver("FELLA"))} " +
                        ": ${objectMapper.writeValueAsString(threeResolver())}"
            }
        }
    }

    @Test
    fun testWithKGraphQL() {
        setup()
        println(benchmark())
    }

    @Test
    fun testNoKGraphQL() {
        withKGraphQL = false
        println(benchmark())
    }
}
