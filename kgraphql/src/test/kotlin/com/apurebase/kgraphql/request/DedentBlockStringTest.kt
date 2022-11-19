package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.schema.structure.dedentBlockStringValue
import com.apurebase.kgraphql.schema.structure.getBlockStringIndentation
import com.apurebase.kgraphql.schema.structure.printBlockString
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DedentBlockStringTest {

    private fun joinLines(vararg lines: String) = lines.joinToString("\n")

    ////////////////////////////////////////////
    ////////// dedentBlockStringValue //////////
    ////////////////////////////////////////////
    @Test
    fun `removes uniform indentation from a string`() {
        val rawValue = joinLines(
            "",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL."
        )
        dedentBlockStringValue(rawValue) shouldBeEqualTo joinLines(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        )
    }

    @Test
    fun `removes empty leading and trailing lines`() {
        val rawValue = joinLines(
            "",
            "",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL.",
            "",
            ""
        )
        dedentBlockStringValue(rawValue) shouldBeEqualTo joinLines(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        )
    }

    @Test
    fun `removes blank leading and trailing lines`() {
        val rawValue = joinLines(
            "  ",
            "        ",
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL.",
            "        ",
            "  "
        )
        dedentBlockStringValue(rawValue) shouldBeEqualTo joinLines(
            "Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        )
    }

    @Test
    fun `retains indentation from first line`() {
        val rawValue = joinLines(
            "    Hello,",
            "      World!",
            "",
            "    Yours,",
            "      GraphQL."
        )
        dedentBlockStringValue(rawValue) shouldBeEqualTo joinLines(
            "    Hello,",
            "  World!",
            "",
            "Yours,",
            "  GraphQL."
        )
    }

    @Test
    fun `does not alter trailing spaces`() {
        val rawValue = joinLines(
            "               ",
            "    Hello,     ",
            "      World!   ",
            "               ",
            "    Yours,     ",
            "      GraphQL. ",
            "               "
        )
        dedentBlockStringValue(rawValue) shouldBeEqualTo joinLines(
            "Hello,     ",
            "  World!   ",
            "           ",
            "Yours,     ",
            "  GraphQL. "
        )
    }


    ///////////////////////////////////////////
    //////// getBlockStringIndentation ////////
    ///////////////////////////////////////////
    @Test
    fun `returns zero for an empty array`() {
        getBlockStringIndentation(listOf()) shouldBeEqualTo 0
    }

    @Test
    fun `do not take first line into account`() {
        getBlockStringIndentation(listOf("  a")) shouldBeEqualTo 0
        getBlockStringIndentation(listOf(" a", "  b")) shouldBeEqualTo 2
    }

    @Test
    fun `returns minimal indentation length`() {
        getBlockStringIndentation(listOf("", " a", "  b")) shouldBeEqualTo 1
        getBlockStringIndentation(listOf("", "  a", " b")) shouldBeEqualTo 1
        getBlockStringIndentation(listOf("", "  a", " b", "c")) shouldBeEqualTo 0
    }

    @Test
    fun `count both tab and space as single character`() {
        getBlockStringIndentation(listOf("", "\ta", "          b")) shouldBeEqualTo 1
        getBlockStringIndentation(
            listOf(
                "",
                "\t a",
                "          b"
            )
        ) shouldBeEqualTo 2
        getBlockStringIndentation(
            listOf(
                "",
                " \t a",
                "          b"
            )
        ) shouldBeEqualTo 3
    }

    @Test
    fun `do not take empty lines into account`() {
        getBlockStringIndentation(listOf("a", "\t")) shouldBeEqualTo 0
        getBlockStringIndentation(listOf("a", " ")) shouldBeEqualTo 0
        getBlockStringIndentation(listOf("a", " ", "  b")) shouldBeEqualTo 2
        getBlockStringIndentation(listOf("a", " ", "  b")) shouldBeEqualTo 2
        getBlockStringIndentation(listOf("a", "", " b")) shouldBeEqualTo 1
    }

    //////////////////////////////////////////
    //////////// printBlockString ////////////
    //////////////////////////////////////////
    @Test
    fun `by default print block strings as single line`() {
        val str = "one liner"
        printBlockString(str) shouldBeEqualTo "\"\"\"one liner\"\"\""
        printBlockString(str, "", true) shouldBeEqualTo "\"\"\"\none liner\n\"\"\""
    }

    @Test
    fun `correctly prints single-line with leading space`() {
        val str = "    space-led string"
        printBlockString(str) shouldBeEqualTo "\"\"\"    space-led string\"\"\""
        printBlockString(str, "", true) shouldBeEqualTo "\"\"\"    space-led string\n\"\"\""
    }

    @Test
    fun `correctly prints single-line with leading space and quotation`() {
        val str = "    space-led value \"quoted string\""

        printBlockString(str) shouldBeEqualTo "\"\"\"    space-led value \"quoted string\"\n\"\"\""
        printBlockString(str, "", true) shouldBeEqualTo "\"\"\"    space-led value \"quoted string\"\n\"\"\""
    }

    @Test
    fun `correctly prints string with a first line indentation`() {
        val str = joinLines(
            "    first  ",
            "  line     ",
            "indentation",
            "     string"
        )

        printBlockString(str) shouldBeEqualTo joinLines(
            "\"\"\"",
            "    first  ",
            "  line     ",
            "indentation",
            "     string",
            "\"\"\""
        )
    }

}
