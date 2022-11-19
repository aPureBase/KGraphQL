package com.apurebase.kgraphql.access

import com.apurebase.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class AccessRulesTest {

    class Player(val name: String, val id: Int = 0)

    val schema = defaultSchema {
        query("black_mamba") {
            resolver { -> Player("KOBE") }
            accessRule { ctx -> if (ctx.get<String>().equals("LAKERS")) null else IllegalAccessException() }
        }

        query("white_mamba") {
            resolver { -> Player("BONNER") }
        }

        type<Player> {
            val accessRuleBlock = { player: Player, _: Context ->
                if (player.name != "BONNER") IllegalAccessException("ILLEGAL ACCESS") else null
            }
            property(Player::id) {
                accessRule(accessRuleBlock)
            }
            property<String>("item") {
                accessRule(accessRuleBlock)
                resolver { "item" }
            }
        }
    }


    @Test
    fun `allow when matching`() {
        val kobe = deserialize(
            schema.executeBlocking("{black_mamba{name}}", context = context { +"LAKERS" })
        ).extract<String>("data/black_mamba/name")

        assertThat(kobe, equalTo("KOBE"))
    }

    @Test
    fun `reject when not matching`() {
        expect<IllegalAccessException>("") {
            deserialize(
                schema.executeBlocking("{ black_mamba {id} }", context = context { +"LAKERS" })
            ).extract<String>("data/black_mamba/id")
        }
    }

    @Test
    fun `allow property resolver access rule`() {
        assertThat(
            deserialize(schema.executeBlocking("{white_mamba {item}}")).extract<String>("data/white_mamba/item"),
            equalTo("item")
        )
    }

    @Test
    fun `reject property resolver access rule`() {
        expect<IllegalAccessException>("ILLEGAL ACCESS") {
            schema.executeBlocking("{black_mamba {item}}", context = context { +"LAKERS" }).also(::println)
        }
    }

    //TODO: MORE TESTS

}
