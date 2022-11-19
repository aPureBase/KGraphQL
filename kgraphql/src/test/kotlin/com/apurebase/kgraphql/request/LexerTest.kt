package com.apurebase.kgraphql.request

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.schema.model.ast.Source
import com.apurebase.kgraphql.schema.model.ast.Token
import com.apurebase.kgraphql.schema.model.ast.TokenKindEnum.*
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

internal class LexerTest {

    private fun lexOne(str: String, block: Token.() -> Unit = { }) {
        block(Lexer(str).advance())
    }

    private fun lexSecond(str: String, block: Token.() -> Unit = { }) {
        block(Lexer(str).also { it.advance() }.advance())
    }

    private fun shouldThrowSyntaxError(str: String, expect: GraphQLError.() -> Unit) {
        val result = invoking { lexSecond(str) } shouldThrow GraphQLError::class
        expect(result.exception)
    }

    @Test
    fun `disallows uncommon control characters`() {
        invoking { lexSecond("\u0007") }
            .shouldThrow(GraphQLError::class)
            .withMessage("Syntax Error: Cannot contain the invalid character \"\\u0007\".")
    }

    @Test
    fun `accepts BOM header`() {
        lexOne("\uFEFF foo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 2
            end shouldBeEqualTo 5
            value shouldBeEqualTo "foo"
        }
    }

    @Test
    fun `tracks line breaks`() {
        lexOne("foo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 0
            end shouldBeEqualTo 3
            line shouldBeEqualTo 1
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\nfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 1
            end shouldBeEqualTo 4
            line shouldBeEqualTo 2
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\rfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 1
            end shouldBeEqualTo 4
            line shouldBeEqualTo 2
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\r\nfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 2
            end shouldBeEqualTo 5
            line shouldBeEqualTo 2
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\n\rfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 2
            end shouldBeEqualTo 5
            line shouldBeEqualTo 3
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\r\r\n\nfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 4
            end shouldBeEqualTo 7
            line shouldBeEqualTo 4
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
        lexOne("\n\n\r\rfoo") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 4
            end shouldBeEqualTo 7
            line shouldBeEqualTo 5
            column shouldBeEqualTo 1
            value shouldBeEqualTo "foo"
        }
    }

    @Test
    fun `records line and column`() {
        lexOne("\n \r\n \r  foo\n") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 8
            end shouldBeEqualTo 11
            line shouldBeEqualTo 4
            column shouldBeEqualTo 3
            value shouldBeEqualTo "foo"
        }
    }

    @Test
    fun `skips whitespace and comments`() {
        lexOne(
            """

    foo


"""
        ) {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 6
            end shouldBeEqualTo 9
            value shouldBeEqualTo "foo"
        }
        lexOne(
            """
    #comment
    foo#comment
"""
        ) {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 18
            end shouldBeEqualTo 21
            value shouldBeEqualTo "foo"
        }
        lexOne(",,,foo,,,") {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 3
            end shouldBeEqualTo 6
            value shouldBeEqualTo "foo"
        }
    }

    @Test
    fun `errors respect whitespace`() {
        val result = invoking {
            lexOne(listOf("", "", "    ?", "").joinToString("\n"))
        } shouldThrow GraphQLError::class

        result.exception.prettyPrint() shouldBeEqualTo """
                  Syntax Error: Cannot parse the unexpected character "?".
                  
                  GraphQL request:3:5
                  2 |
                  3 |     ?
                    |     ^
                  4 |
            """.trimIndent()
    }

    @Test
    fun `updates line numbers in error for file context`() {
        val result = invoking {
            val str = listOf("", "", "     ?", "").joinToString("\n")
            val source = Source(
                str,
                "foo.graphql",
                Source.LocationSource(11, 12)
            )
            Lexer(source).advance()
        } shouldThrow GraphQLError::class

        result.exception.prettyPrint() shouldBeEqualTo """
                Syntax Error: Cannot parse the unexpected character "?".
                
                foo.graphql:13:6
                12 |
                13 |      ?
                   |      ^
                14 |
            """.trimIndent()

    }

    @Test
    fun `updates column numbers in error for file context`() {
        val result = invoking {
            val source = Source(
                "?",
                "foo.graphql",
                Source.LocationSource(1, 5)
            )
            Lexer(source).advance()
        } shouldThrow GraphQLError::class

        result.exception.prettyPrint() shouldBeEqualTo """
                Syntax Error: Cannot parse the unexpected character "?".
                
                foo.graphql:1:5
                1 |     ?
                  |     ^
            """.trimIndent()
    }

    @Test
    fun `lexes strings`() {
        lexOne("\"\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 2
            value shouldBeEqualTo ""
        }

        lexOne("\"simple\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 8
            value shouldBeEqualTo "simple"
        }

        lexOne("\" white space \"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 15
            value shouldBeEqualTo " white space "
        }

        lexOne("\"quote \\\"\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 10
            value shouldBeEqualTo "quote \""
        }

        lexOne("\"escaped \\n\\r\\b\\t\\f\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 20
            value shouldBeEqualTo "escaped \n\r\b\tf"
        }

        lexOne("\"slashes \\\\ \\/\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 15
            value shouldBeEqualTo "slashes \\ /"
        }

        lexOne("\"unicode \\u1234\\u5678\\u90AB\\uCDEF\"") {
            kind shouldBeEqualTo STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 34
            value shouldBeEqualTo "unicode \u1234\u5678\u90AB\uCDEF"
        }
    }

    @Test
    fun `lex reports useful string errors`() {
        shouldThrowSyntaxError("\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("\"\"\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 4
            }
        }

        shouldThrowSyntaxError("\"\"\"\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 5
            }
        }

        shouldThrowSyntaxError("\"no end quote") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 14
            }
        }

        shouldThrowSyntaxError("'single quotes'") {
            message shouldBeEqualTo "Syntax Error: Unexpected single quote character (\'), did you mean to use a double quote (\")?"
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("\"contains unescaped \u0007 control char\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character within String: \"\\u0007\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 21
            }
        }

        shouldThrowSyntaxError("\"null-byte is not \u0000 end of file\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character within String: \"\\u0000\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 19
            }
        }

        shouldThrowSyntaxError("\"multi\nline\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"multi\rline\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\z esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\z."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\x esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\x."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\u1 esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\u1 es."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\u0XX1 esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\u0XX1."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\uXXXX esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\uXXXX."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\uFXXX esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\uFXXX."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }

        shouldThrowSyntaxError("\"bad \\uXXXF esc\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character escape sequence: \\uXXXF."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 7
            }
        }
    }

    @Test
    fun `lexes block strings`() {
        lexOne("\"\"\"\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 6
            value shouldBeEqualTo ""
        }

        lexOne("\"\"\"simple\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 12
            value shouldBeEqualTo "simple"
        }

        lexOne("\"\"\" white space \"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 19
            value shouldBeEqualTo " white space "
        }

        lexOne("\"\"\"contains \" quote\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 22
            value shouldBeEqualTo "contains \" quote"
        }

        lexOne("\"\"\"contains \\\"\"\" triplequote\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 31
            value shouldBeEqualTo "contains \"\"\" triplequote"
        }

        lexOne("\"\"\"multi\nline\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 16
            value shouldBeEqualTo "multi\nline"
        }

        lexOne("\"\"\"multi\rline\r\nnormalized\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 28
            value shouldBeEqualTo "multi\nline\nnormalized"
        }

        lexOne("\"\"\"unescaped \\n\\r\\b\\t\\f\\u1234\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 32
            value shouldBeEqualTo "unescaped \\n\\r\\b\\t\\f\\u1234"
        }

        lexOne("\"\"\"slashes \\\\ \\/\"\"\"") {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 19
            value shouldBeEqualTo "slashes \\\\ \\/"
        }


        lexOne(
            """""${'"'}

        spans
          multiple
            lines

        ""${'"'}"""
        ) {
            kind shouldBeEqualTo BLOCK_STRING
            start shouldBeEqualTo 0
            end shouldBeEqualTo 68
            value shouldBeEqualTo "spans\n  multiple\n    lines"
        }
    }

    @Test
    fun `advance line after lexing multiline block string`() {
        val str = """""${'"'}

        spans
          multiple
            lines

        ${'\n'} ""${'"'} second_token"""
        lexSecond(str) {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 71
            end shouldBeEqualTo 83
            line shouldBeEqualTo 8
            column shouldBeEqualTo 6
            value shouldBeEqualTo "second_token"
        }


        val str2 = listOf(
            "\"\"\" \n",
            "spans \r\n",
            "multiple \n\r",
            "lines \n\n",
            "\"\"\"\n second_token"
        ).joinToString("")
        lexSecond(str2) {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 37
            end shouldBeEqualTo 49
            line shouldBeEqualTo 8
            column shouldBeEqualTo 2
            value shouldBeEqualTo "second_token"
        }
    }

    @Test
    fun `lex reports useful block string errors`() {
        shouldThrowSyntaxError("\"\"\"") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 4
            }
        }

        shouldThrowSyntaxError("\"\"\"no end quote") {
            message shouldBeEqualTo "Syntax Error: Unterminated string."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 16
            }
        }

        shouldThrowSyntaxError("\"\"\"contains unescaped \u0007 control char\"\"\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character within String: \"\\u0007\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 23
            }
        }

        shouldThrowSyntaxError("\"\"\"null-byte is not \u0000 end of file\"\"\"") {
            message shouldBeEqualTo "Syntax Error: Invalid character within String: \"\\u0000\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 21
            }
        }
    }

    @Test
    fun `lexes numbers`() {
        lexOne("4") {
            kind shouldBeEqualTo INT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo "4"
        }

        lexOne("4.123") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 5
            value shouldBeEqualTo "4.123"
        }

        lexOne("-4") {
            kind shouldBeEqualTo INT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 2
            value shouldBeEqualTo "-4"
        }

        lexOne("9") {
            kind shouldBeEqualTo INT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo "9"
        }

        lexOne("0") {
            kind shouldBeEqualTo INT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo "0"
        }

        lexOne("-4.123") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 6
            value shouldBeEqualTo "-4.123"
        }

        lexOne("0.123") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 5
            value shouldBeEqualTo "0.123"
        }

        lexOne("123e4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 5
            value shouldBeEqualTo "123e4"
        }

        lexOne("123E4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 5
            value shouldBeEqualTo "123E4"
        }

        lexOne("123e-4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 6
            value shouldBeEqualTo "123e-4"
        }

        lexOne("123e+4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 6
            value shouldBeEqualTo "123e+4"
        }

        lexOne("-1.123e4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 8
            value shouldBeEqualTo "-1.123e4"
        }

        lexOne("-1.123E4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 8
            value shouldBeEqualTo "-1.123E4"
        }

        lexOne("-1.123e-4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 9
            value shouldBeEqualTo "-1.123e-4"
        }

        lexOne("-1.123e+4") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 9
            value shouldBeEqualTo "-1.123e+4"
        }

        lexOne("-1.123e4567") {
            kind shouldBeEqualTo FLOAT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 11
            value shouldBeEqualTo "-1.123e4567"
        }
    }

    @Test
    fun `lex reports useful number errors`() {
        shouldThrowSyntaxError("00") {
            message shouldBeEqualTo "Syntax Error: Invalid number, unexpected digit after 0: \"0\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("01") {
            message shouldBeEqualTo "Syntax Error: Invalid number, unexpected digit after 0: \"1\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("01.23") {
            message shouldBeEqualTo "Syntax Error: Invalid number, unexpected digit after 0: \"1\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("+1") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"+\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("1.") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: <EOF>."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }

        shouldThrowSyntaxError("1e") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: <EOF>."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }

        shouldThrowSyntaxError("1E") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: <EOF>."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }

        shouldThrowSyntaxError("1.e1") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"e\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }

        shouldThrowSyntaxError(".123") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \".\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("1.A") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"A\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }

        shouldThrowSyntaxError("-A") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"A\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("1.0e") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: <EOF>."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 5
            }
        }

        shouldThrowSyntaxError("1.0eA") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"A\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 5
            }
        }

        shouldThrowSyntaxError("1.2e3e") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"e\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 6
            }
        }

        shouldThrowSyntaxError("1.2e3.4") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \".\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 6
            }
        }

        shouldThrowSyntaxError("1.23.4") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \".\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 5
            }
        }
    }

    @Test
    fun `lex does not allow name-start after a number`() {
        shouldThrowSyntaxError("0xF1") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"x\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("0b10") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"b\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("123abc") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"a\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 4
            }
        }

        shouldThrowSyntaxError("1_234") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"_\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("1ß") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"\\u00DF\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }

        shouldThrowSyntaxError("1.23f") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"f\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 5
            }
        }

        shouldThrowSyntaxError("1.234_5") {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"_\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 6
            }
        }

        shouldThrowSyntaxError("1ß") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"\\u00DF\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 2
            }
        }
    }

    @Test
    fun `lexes punctuation`() {
        lexOne("!") {
            kind shouldBeEqualTo BANG
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("$") {
            kind shouldBeEqualTo DOLLAR
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("(") {
            kind shouldBeEqualTo PAREN_L
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne(")") {
            kind shouldBeEqualTo PAREN_R
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("...") {
            kind shouldBeEqualTo SPREAD
            start shouldBeEqualTo 0
            end shouldBeEqualTo 3
            value shouldBeEqualTo null
        }

        lexOne(":") {
            kind shouldBeEqualTo COLON
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("=") {
            kind shouldBeEqualTo EQUALS
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("@") {
            kind shouldBeEqualTo AT
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("[") {
            kind shouldBeEqualTo BRACKET_L
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("]") {
            kind shouldBeEqualTo BRACKET_R
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("{") {
            kind shouldBeEqualTo BRACE_L
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("|") {
            kind shouldBeEqualTo PIPE
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }

        lexOne("}") {
            kind shouldBeEqualTo BRACE_R
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo null
        }
    }

    @Test
    fun `lex reports useful unknown character error`() {
        shouldThrowSyntaxError("..") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \".\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("?") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"?\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("\u203B") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"\\u203B\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }

        shouldThrowSyntaxError("\u200b") {
            message shouldBeEqualTo "Syntax Error: Cannot parse the unexpected character \"\\u200B\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 1
            }
        }
    }

    @Test
    fun `lex reports useful information for dashes in names`() {
        val source = Source("a-b")
        val lexer = Lexer(source)
        val firstToken = lexer.advance()

        firstToken.run {
            kind shouldBeEqualTo NAME
            start shouldBeEqualTo 0
            end shouldBeEqualTo 1
            value shouldBeEqualTo "a"
        }

        val result = invoking { lexer.advance() } shouldThrow GraphQLError::class

        result.exception.run {
            message shouldBeEqualTo "Syntax Error: Invalid number, expected digit but got: \"b\"."
            locations!!.first().run {
                line shouldBeEqualTo 1
                column shouldBeEqualTo 3
            }
        }
    }

    @Test
    fun `produces double linked list of tokens, including comments`() {
        val source = Source(
            """
            {
                #comment
                field
            }
        """
        )

        val lexer = Lexer(source)
        val startToken = lexer.token
        var endToken: Token?

        do {
            endToken = lexer.advance()
            // Lexer advances over ignored comment tokens to make writing parsers
            // easier, but will include them in the linked list result.
            endToken.kind shouldNotEqual COMMENT
        } while (endToken?.kind != EOF)

        startToken.prev shouldBeEqualTo null
        endToken.next shouldBeEqualTo null

        val tokens = mutableListOf<Token>()
        var tok: Token? = startToken
        while (tok != null) {
            tokens.add(tok)
            tok = tok.next ?: break
            if (tokens.size != 0) {
                tok.prev shouldBeEqualTo tokens[tokens.size - 1]
            }
        }

        tokens.map { it.kind } shouldEqual listOf(
            SOF,
            BRACE_L,
            COMMENT,
            NAME,
            BRACE_R,
            EOF
        )
    }


    // ---------------------------------------------//
    // ----------- isPunctuatorTokenKind -----------//
    // ---------------------------------------------//
    private fun isPunctuatorToken(text: String): Boolean {
        return Lexer(text).advance().kind.isPunctuatorTokenKind
    }

    @Test
    fun `returns true for punctuator tokens`() {
        isPunctuatorToken("!") shouldBeEqualTo true
        isPunctuatorToken("$") shouldBeEqualTo true
        isPunctuatorToken("&") shouldBeEqualTo true
        isPunctuatorToken("(") shouldBeEqualTo true
        isPunctuatorToken(")") shouldBeEqualTo true
        isPunctuatorToken("...") shouldBeEqualTo true
        isPunctuatorToken(":") shouldBeEqualTo true
        isPunctuatorToken("=") shouldBeEqualTo true
        isPunctuatorToken("@") shouldBeEqualTo true
        isPunctuatorToken("[") shouldBeEqualTo true
        isPunctuatorToken("]") shouldBeEqualTo true
        isPunctuatorToken("{") shouldBeEqualTo true
        isPunctuatorToken("|") shouldBeEqualTo true
        isPunctuatorToken("}") shouldBeEqualTo true
    }

    @Test
    fun `returns false for non-punctuator tokens`() {
        isPunctuatorToken("") shouldBeEqualTo false
        isPunctuatorToken("name") shouldBeEqualTo false
        isPunctuatorToken("1") shouldBeEqualTo false
        isPunctuatorToken("3.14") shouldBeEqualTo false
        isPunctuatorToken("\"str\"") shouldBeEqualTo false
        isPunctuatorToken("\"\"\"str\"\"\"") shouldBeEqualTo false
    }

}
