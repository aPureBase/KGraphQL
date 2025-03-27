package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.execution.Executor
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.testing.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KtorConfigurationTest: KtorTest() {

    @Test
    fun `default configuration should`() {
        var checked = false
        testApplication {
            install(GraphQL) {}
            application {
                val plugin = plugin(GraphQL)
                plugin.schema.configuration.executor shouldBeEqualTo Executor.Parallel
            }
            checked = true
        }
        checked shouldBeEqualTo true
    }

    @Test
    fun `update configuration`() {
        var checked = false
        testApplication {
            install(GraphQL) {
                executor = Executor.DataLoaderPrepared
            }
            application {
                val plugin = plugin(GraphQL)
                plugin.schema.configuration.executor shouldBeEqualTo Executor.DataLoaderPrepared
            }
            checked = true
        }
        checked shouldBeEqualTo true
    }

}
