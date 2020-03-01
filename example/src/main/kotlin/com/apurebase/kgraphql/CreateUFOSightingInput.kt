package com.apurebase.kgraphql

import com.apurebase.kgraphql.model.UFOSighting
import java.time.LocalDate

fun CreateUFOSightingInput.toUFOSighting() : UFOSighting {
    return UFOSighting(
            dateSighting = this.date,
            city = this.country[0].city,
            state = this.country[0].state,
            country = this.country[0].country,
            shape = this.shape,
            duration = this.duration ?: 0.0,
            comments = this.country[0].comments,
            latitude = this.latitude ?: 0.0,
            longitude = this.longitude ?: 0.0
    )
}

data class CreateUFOSightingInput(
        var date: LocalDate = LocalDate.now(),
        var country: List<CountryInput> = emptyList(),
        var shape: String = "",
        var duration: Double = 0.0,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
)


data class CountryInput(
        var city: String = "",
        var state: String = "",
        var country: String = "",
        var comments: String = ""
)