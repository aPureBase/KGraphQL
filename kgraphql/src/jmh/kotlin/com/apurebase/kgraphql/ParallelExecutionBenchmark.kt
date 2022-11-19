package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.Schema
import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1)
open class ParallelExecutionBenchmark {

    @Param("true", "false")
    var withSuspendResolvers = false

    var schema: Schema = KGraphQL.schema { }

    @Setup
    fun setup() {
        schema = KGraphQL.schema {

            if (withSuspendResolvers == false)
                repeat(1000) {
                    query("automated-$it") {
                        resolver { ->
                            Thread.sleep(3)
                            "$it"
                        }
                    }
                } else {
                repeat(1000) {
                    query("automated-$it") {
                        resolver { ->
                            delay(3)
                            "$it"
                        }
                    }
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    fun queryBenchmark(): String =
        schema.executeBlocking("{ " + (0..999).map { "automated-${it}" }.joinToString(", ") + " }")

    @Test
    fun benchmarkWithThreads() {
        setup()
        println(queryBenchmark())
    }

    @Test
    fun benchmarkWithSuspendResolvers() {
        withSuspendResolvers = true
        setup()
        println(queryBenchmark())
    }
}
