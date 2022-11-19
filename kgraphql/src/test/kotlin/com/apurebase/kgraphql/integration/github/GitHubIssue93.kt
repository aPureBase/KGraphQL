package com.apurebase.kgraphql.integration.github

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.KGraphQL
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.with
import org.junit.jupiter.api.Test

data class MyInput(val value1: String)

class GitHubIssue93 {

    @Test
    fun `Incorrect input Parameter should throw an appropriate exception`() {
        val schema = KGraphQL.schema {
            query("main") {
                resolver { input: MyInput -> "${input.value1}!!!" }
            }
        }

        invoking {
            schema.executeBlocking(
                """
        {
            main(input: { valu1: "Hello" })
        }
    """
            )
        } shouldThrow GraphQLError::class with {
            message shouldBeEqualTo "Constructor Parameter 'valu1' can not be found in 'MyInput'"
        }
    }
}
