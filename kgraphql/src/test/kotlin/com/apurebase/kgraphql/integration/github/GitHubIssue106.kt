package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.KGraphQL
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.with
import org.junit.jupiter.api.Test

class GitHubIssue106 {

    @Test
    fun `Java class as inputType should throw an appropriate exception`() {
        val schema = KGraphQL.schema {
            query("test") {
                resolver { radius: Double, location: LatLng ->
                    "Hello $radius. ${location.lat} ${location.lng}"
                }
            }
        }

        invoking {
            schema.executeBlocking(
                """
                {
                    test(radius: 2.3, location: { lat: 2.3, lng: 2.3 })
                }
            """
            )
        } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "Java class 'LatLng' as inputType are not supported"
        }

    }
}
