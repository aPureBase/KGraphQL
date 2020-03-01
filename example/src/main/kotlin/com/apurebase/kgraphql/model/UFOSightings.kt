package com.apurebase.kgraphql.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.date
import java.time.LocalDate

object UFOSightings : Table() {
    val id = integer("id").autoIncrement()
    val dateSighting = date("seeing_at")
    val city = varchar("city", 250)
    val state = varchar("state", 250)
    val country = varchar("country", 250)
    val shape = varchar("shape", 250)
    val duration = double("duration")
    val comments = varchar("comments", 250)
    val latitude = double("latitude")
    val longitude = double("longitude")

    override val primaryKey = PrimaryKey(id, name = "PK_UFOSighting_ID")
    val uniqueIndex = uniqueIndex("IDX_UFOSighting_UNIQUE", dateSighting, city, state, country, shape, duration, comments)

}

data class UFOSighting (
        var id: Int = -1,
        var dateSighting: LocalDate = LocalDate.now(),
        var city: String = "",
        var state: String = "",
        var country: String = "",
        var shape: String = "",
        var duration: Double = 0.0,
        var comments: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
)

