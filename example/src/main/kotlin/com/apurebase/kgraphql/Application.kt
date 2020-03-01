package com.apurebase.kgraphql

import com.apurebase.kgraphql.dao.DatabaseFactory
import com.apurebase.kgraphql.exception.NotFoundException
import com.apurebase.kgraphql.model.*
import com.apurebase.kgraphql.service.UFOSightingService
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import java.time.LocalDate

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@UnstableDefault
@KtorExperimentalAPI
fun Application.module() {
    install(DefaultHeaders)
    install(CORS) {
        method(HttpMethod.Post)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        anyHost()
        maxAgeInSeconds = CORS.CORS_DEFAULT_MAX_AGE
    }
    DatabaseFactory.init()
    val service = UFOSightingService()
    routing {
        graphql {

            configure {
                useDefaultPrettyPrinter = true
            }

            stringScalar<LocalDate> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> LocalDate.parse(dateString) }
            }

            query("sightings") {
                description = "Returns a subset of the UFO Sighting records"

                resolver { size: Int? -> service.findAll(size ?: 10).toMutableList() }.withArgs {
                    arg<Int> { name = "size"; defaultValue = 10; description = "The number of records to return" }
                }
            }

            query("sighting") {
                description = "Returns a single UFO Sighting record based on the id"

                resolver { id: Int ->
                    service.findById(id) ?: throw NotFoundException("Sighting with id: $id does not exist")
                }
            }

            query("user") {
                description = "Returns a single User based on the id"

                resolver { id: Int ->
                    users.getOrNull(id - 1) ?: throw NotFoundException("User with id: $id does not exist")
                }
            }

            type<User> {
                description = "A User who has reported a UFO sighting"

                property<UFOSighting?>("sighting") {
                    resolver { user -> service.findById(user.id) }
                }
            }

            mutation("createUFOSighting") {
                description = "Adds a new UFO Sighting to the database"

                resolver {
                    input: CreateUFOSightingInput -> service.create(input.toUFOSighting())
                }

            }

            query("topSightings") {
                description = "Returns a list of the top state,country based on the number of sightings"

                resolver { -> service.getTopSightings() }
            }

            query("topCountrySightings") {
                description = "Returns a list of the top countries based on the number of sightings"

                resolver { -> service.getTopCountrySightings() }
            }

            type<CountrySightings> {
                description = "A country sighting; contains total number of occurrences"

                property(CountrySightings::numOccurrences) {
                    description = "The number of occurrences of the sighting"
                }
            }

            inputType<CreateUFOSightingInput>()

            type<UFOSighting> {
                description = "A UFO sighting"

                property(UFOSighting::dateSighting) {
                    description = "The date of the sighting"
                }

                property<User>("user") {
                    resolver {
                        users[(0..2).shuffled().last()]
                    }
                }
            }
        }
    }
}