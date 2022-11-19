package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.Schema
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class RequestCachingBenchmark {

    @Param("true", "false")
    var caching = true

    lateinit var schema: Schema

    @Setup
    fun setup() {
        schema = BenchmarkSchema.create {
            configure {
                useCachingDocumentParser = caching
            }
        }
    }

    @Benchmark
    fun benchmark(): String {
        return schema.execute("{one{name, quantity, active}, two(name : \"FELLA\"){range{start, endInclusive}}, three{id}}")
    }
}