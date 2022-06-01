package com.apurebase.kgraphql

import com.apurebase.kgraphql.model.User
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(Authentication) {
        basic {
            realm = "ktor"
            validate {
                User(4, it.name)
            }
        }
    }

    install(GraphQL) {
        useDefaultPrettyPrinter = true
        playground = true
        /**
         * Change 2: Added debug option to GraphQL Configuration (flag to output exception information to extensions)
         */
        debug = false
        endpoint = "/"

        wrap {
            authenticate(optional = true, build = it)
        }

        context { call ->
            call.authentication.principal<User>()?.let {
                +it
            }
        }

        schema { ufoSchema() }
    }
}
