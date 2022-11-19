package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.integration.BaseSchemaTest
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("3.2 Directives")
class DirectivesSpecificationTest : BaseSchemaTest() {

    @Test
    fun `query with @include directive on field`() {
        val map = execute("{film{title, year @include(if: false)}}")
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }

    @Test
    fun `query with @skip directive on field`() {
        val map = execute("{film{title, year @skip(if: true)}}")
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }

    @Test
    fun `query with @include and @skip directive on field`() {
        val mapBothSkip = execute("{film{title, year @include(if: false) @skip(if: true)}}")
        assertThat(extractOrNull(mapBothSkip, "data/film/year"), nullValue())

        val mapOnlySkip = execute("{film{title, year @include(if: true) @skip(if: true)}}")
        assertThat(extractOrNull(mapOnlySkip, "data/film/year"), nullValue())

        val mapOnlyInclude = execute("{film{title, year @include(if: false) @skip(if: false)}}")
        assertThat(extractOrNull(mapOnlyInclude, "data/film/year"), nullValue())

        val mapNeither = execute("{film{title, year @include(if: true) @skip(if: false)}}")
        assertThat(extractOrNull(mapNeither, "data/film/year"), notNullValue())
    }

    @Test
    fun `query with @include and @skip directive on field object`() {
        val mapWithSkip = execute("{ number(big: true), film @skip(if: true) { title } }")
        mapWithSkip.extract<String?>("data/film") shouldBeEqualTo null

        val mapWithoutSkip = execute("{ number(big: true), film @skip(if: false) { title } }")
        mapWithoutSkip.extract<String>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithInclude = execute("{ number(big: true), film @include(if: true) { title } }")
        mapWithInclude.extract<String?>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithoutInclude = execute("{ number(big: true), film @include(if: false) { title } }")
        mapWithoutInclude.extract<String>("data/film") shouldBeEqualTo null
    }

    @Test
    fun `query with @include directive on field with variable`() {
        val map = execute(
            "query film (\$include: Boolean!) {film{title, year @include(if: \$include)}}",
            "{\"include\":\"false\"}"
        )
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }
}
