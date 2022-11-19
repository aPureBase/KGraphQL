package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.model.FunctionWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

/**
 * Contrary to Java 8 which has about 43 different specialized function interfaces
 * to avoid boxing and unboxing as much as possible, the Function objects compiled by Kotlin
 * only implement fully generic interfaces, effectively using the Object type for any input or output value.
 *
 * This benchmark proves, that it makes no performance difference?
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class FunctionExecutionBenchmark {

    @Param("true", "false")
    var useFunctionWrapper = true

    lateinit var functionWrapper: FunctionWrapper.ArityTwo<String, Int, String>

    lateinit var biFunction: BiFunction<Int, String, String>

    val arg1 = 3

    val arg2 = "CODE"

    val implementation = { int: Int, string: String -> "${int * ThreadLocalRandom.current().nextDouble()} $string" }

    @Setup
    fun setup() {
        if (useFunctionWrapper) {
            val implSuspend: suspend (Int, String) -> String = { int, string -> implementation(int, string) }
            functionWrapper = FunctionWrapper.ArityTwo(implSuspend, false)
        } else {
            biFunction = BiFunction(implementation)
        }
    }

    @Benchmark
    fun benchmarkFunctionExecution(): String? {
        return if (useFunctionWrapper) {
            runBlocking {
                functionWrapper.invoke(arg1, arg2)
            }
        } else {
            biFunction.apply(arg1, arg2)
        }
    }

    @Test
    fun check() {
        setup()
        println(benchmarkFunctionExecution())
    }
}
