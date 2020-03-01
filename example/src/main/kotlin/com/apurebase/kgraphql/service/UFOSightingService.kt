package com.apurebase.kgraphql.service

import com.apurebase.kgraphql.dao.DatabaseFactory.dbQuery
import com.apurebase.kgraphql.dao.execAndMap
import com.apurebase.kgraphql.model.CountrySightings
import com.apurebase.kgraphql.model.UFOSighting
import com.apurebase.kgraphql.model.UFOSightings
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class UFOSightingService {

    suspend fun create(uFOSighting: UFOSighting): UFOSighting {
        var key = 0
        dbQuery {
            key = UFOSightings.insert {
                it[dateSighting] = uFOSighting.dateSighting
                it[city] = uFOSighting.city
                it[state] = uFOSighting.state
                it[country] = uFOSighting.country
                it[shape] = uFOSighting.shape
                it[duration] = uFOSighting.duration
                it[comments] = uFOSighting.comments
                it[latitude] = uFOSighting.latitude
                it[longitude] = uFOSighting.longitude
            } get UFOSightings.id
        }
        uFOSighting.id = key
        return uFOSighting
    }


    suspend fun findAll(size: Int): List<UFOSighting> = dbQuery {
        UFOSightings.selectAll().limit(size, offset = 1).map { toUFOSighting(it) }
    }

    private fun toUFOSighting(row: ResultRow): UFOSighting {
        return UFOSighting(
                id = row[UFOSightings.id],
                dateSighting = row[UFOSightings.dateSighting],
                city = row[UFOSightings.city],
                state = row[UFOSightings.state],
                shape = row[UFOSightings.shape],
                duration = row[UFOSightings.duration],
                comments = row[UFOSightings.comments],
                latitude = row[UFOSightings.latitude],
                longitude = row[UFOSightings.longitude]
        )
    }


    suspend fun findById(id: Int): UFOSighting? {
        return dbQuery {
            UFOSightings.select {
                (UFOSightings.id eq id)
            }.mapNotNull { toUFOSighting(it) }
                    .singleOrNull()
        }
    }

    fun getTopSightings(): MutableList<CountrySightings> {
        val query = "SELECT state, country, COUNT(state) as numOccurrences FROM ufosighting group by state, country order by COUNT(state) DESC"
        return query.execAndMap { rs ->
            CountrySightings(
                    state = rs.getString("state"),
                    country = rs.getString("country"),
                    numOccurrences = rs.getInt("numOccurrences")
            )
        }
    }

    fun getTopCountrySightings(): MutableList<CountrySightings> {
        val query = "SELECT country, COUNT(country) as numOccurrences FROM ufosighting group by country order by COUNT(country) DESC"
        return query.execAndMap { rs ->
            CountrySightings(
                    state = rs.getString("state"),
                    country = rs.getString("country"),
                    numOccurrences = rs.getInt("numOccurrences")
            )
        }
    }
}