package com.apurebase.kgraphql.specification.typesystem

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.integration.BaseSchemaTest
import com.apurebase.kgraphql.schema.execution.ExecutionOptions
import com.apurebase.kgraphql.schema.execution.Executor
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Specification("3.2 Directives")
class DirectivesSpecificationTest : BaseSchemaTest() {

    @Test
    fun `query with @include directive on field`(){
        val map = execute("{film{title, year @include(if: false)}}")
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }

    @Test
    fun `query with @skip directive on field`(){
        val map = execute("{film{title, year @skip(if: true)}}")
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }

    @Test
    fun `query with @include and @skip directive on field`(){
        val mapBothSkip = execute("{film{title, year @include(if: false) @skip(if: true)}}")
        assertThrows<IllegalArgumentException> { mapBothSkip.extract("data/film/year") }

        val mapOnlySkip = execute("{film{title, year @include(if: true) @skip(if: true)}}")
        assertThrows<IllegalArgumentException> { mapOnlySkip.extract("data/film/year") }

        val mapOnlyInclude = execute("{film{title, year @include(if: false) @skip(if: false)}}")
        assertThrows<IllegalArgumentException> { mapOnlyInclude.extract("data/film/year") }

        val mapNeither = execute("{film{title, year @include(if: true) @skip(if: false)}}")
        mapNeither.extract<Int>("data/film/year") shouldBeEqualTo 2006
    }

    @Test
    fun `query with @include and @skip directive on field object`() {
        val mapWithSkip = execute("{ number(big: true), film @skip(if: true) { title } }")
        assertThrows<IllegalArgumentException> { mapWithSkip.extract("data/film") }

        val mapWithoutSkip = execute("{ number(big: true), film @skip(if: false) { title } }")
        mapWithoutSkip.extract<String>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithInclude = execute("{ number(big: true), film @include(if: true) { title } }")
        mapWithInclude.extract<String>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithoutInclude = execute("{ number(big: true), film @include(if: false) { title } }")
        assertThrows<IllegalArgumentException> { mapWithoutInclude.extract("data/film") }
    }

    @Test
    fun `query with @include directive on field with variable`(){
        val map = execute(
                "query film (\$include: Boolean!) {film{title, year @include(if: \$include)}}",
                "{\"include\":\"false\"}"
        )
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }

    @Test
    fun `query with @include directive on field (using DataLoaderPrepared executor)`() {
        val map = execute(
            "{film{title, year @include(if: false)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }

    @Test
    fun `query with @skip directive on field (using DataLoaderPrepared executor)`() {
        val map = execute(
            "{film{title, year @skip(if: true)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }

    @Test
    fun `query with @include and @skip directive on field (using DataLoaderPrepared executor)`() {
        val mapBothSkip = execute(
            "{film{title, year @include(if: false) @skip(if: true)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { mapBothSkip.extract("data/film/year") }

        val mapOnlySkip = execute(
            "{film{title, year @include(if: true) @skip(if: true)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { mapOnlySkip.extract("data/film/year") }

        val mapOnlyInclude = execute(
            "{film{title, year @include(if: false) @skip(if: false)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { mapOnlyInclude.extract("data/film/year") }

        val mapNeither = execute(
            "{film{title, year @include(if: true) @skip(if: false)}}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        mapNeither.extract<Int>("data/film/year") shouldBeEqualTo 2006
    }

    @Test
    fun `query with @include and @skip directive on field object (using DataLoaderPrepared executor)`() {
        val mapWithSkip = execute(
            "{ number(big: true), film @skip(if: true) { title } }",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { mapWithSkip.extract("data/film") }

        val mapWithoutSkip = execute(
            "{ number(big: true), film @skip(if: false) { title } }",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        mapWithoutSkip.extract<String>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithInclude = execute(
            "{ number(big: true), film @include(if: true) { title } }",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        mapWithInclude.extract<String>("data/film/title") shouldBeEqualTo "Prestige"

        val mapWithoutInclude = execute(
            "{ number(big: true), film @include(if: false) { title } }",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { mapWithoutInclude.extract("data/film") }
    }

    @Test
    fun `query with @include directive on field with variable (using DataLoaderPrepared executor)`() {
        val map = execute(
            "query film (\$include: Boolean!) {film{title, year @include(if: \$include)}}",
            "{\"include\":\"false\"}",
            options = ExecutionOptions(executor = Executor.DataLoaderPrepared)
        )
        assertThrows<IllegalArgumentException> { map.extract("data/film/year") }
    }
}
