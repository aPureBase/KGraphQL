package com.apurebase.kgraphql.integration

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.helpers.getFields
import com.apurebase.kgraphql.schema.execution.Execution
import org.amshove.kluent.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class QueryTest : BaseSchemaTest() {
    @Test
    fun `query nested selection set`() {
        val map = execute("{film{title, director{name, age}}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query collection field`() {
        val map = execute("{film{title, director{favActors{name, age}}}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, String>>("data/film/director/favActors[0]"), equalTo(
                mapOf(
                    "name" to prestige.director.favActors[0].name,
                    "age" to prestige.director.favActors[0].age
                )
            )
        )
    }

    @Test
    fun `query scalar field`() {
        val map = execute("{film{id}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/id"), equalTo("${prestige.id.literal}:${prestige.id.numeric}"))
    }

    @Test
    fun `query with selection set on collection`() {
        val map = execute("{film{title, director{favActors{name}}}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, String>>("data/film/director/favActors[0]"),
            equalTo(mapOf("name" to prestige.director.favActors[0].name))
        )
    }

    @Test
    fun `query with selection set on collection 2`() {
        val map = execute("{film{title, director{favActors{age}}}}")
        assertNoErrors(map)
        assertThat(
            map.extract<Map<String, Int>>("data/film/director/favActors[0]"),
            equalTo(mapOf("age" to prestige.director.favActors[0].age))
        )
    }

    @Test
    fun `query with invalid field name`() {
        invoking {
            execute("{film{title, director{name, favDish}}}")
        } shouldThrow GraphQLError::class withMessage "Property favDish on Director does not exist"
    }

    @Test
    fun `query with argument`() {
        val map = execute("{filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with argument 2`() {
        val map = execute("{filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with alias`() {
        val map = execute("{bestFilm: filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/bestFilm/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with field alias`() {
        val map = execute("{filmByRank(rank: 2){fullTitle: title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/filmByRank/fullTitle"), equalTo("Se7en"))
    }

    @Test
    fun `query with multiple aliases`() {
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, secondBestFilm: filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/bestFilm/title"), equalTo("Prestige"))
        assertThat(map.extract<String>("data/secondBestFilm/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with ignored property`() {
        invoking {
            execute("{scenario{author, content}}")
        } shouldThrow GraphQLError::class withMessage "Property author on Scenario does not exist"
    }

    @Test
    fun `query with interface`() {
        val map = execute("{randomPerson{name \n age}}")
        assertThat(
            map.extract<Map<String, String>>("data/randomPerson"), equalTo(
                mapOf(
                    "name" to davidFincher.name,
                    "age" to davidFincher.age
                )
            )
        )
    }

    @Test
    fun `query with collection elements interface`() {
        val map = execute("{people{name, age}}")
        assertThat(
            map.extract<Map<String, String>>("data/people[0]"), equalTo(
                mapOf(
                    "name" to davidFincher.name,
                    "age" to davidFincher.age
                )
            )
        )
    }

    @Test
    fun `query extension property`() {
        val map = execute("{actors{name, age, isOld}}")
        for (i in 0..4) {
            val isOld = map.extract<Boolean>("data/actors[$i]/isOld")
            val age = map.extract<Int>("data/actors[$i]/age")
            assertThat(isOld, equalTo(age > 500))
        }
    }

    @Test
    fun `query extension property with arguments`() {
        val map = execute("{actors{name, picture(big: true)}}")
        for (i in 0..4) {
            val name = map.extract<String>("data/actors[$i]/name").replace(' ', '_')
            assertThat(
                map.extract<String>("data/actors[$i]/picture"),
                equalTo("http://picture.server/pic/$name?big=true")
            )
        }
    }

    @Test
    fun `query extension property with optional argument`() {
        val map = execute("{actors{name, picture}}")
        for (i in 0..4) {
            val name = map.extract<String>("data/actors[$i]/name").replace(' ', '_')
            assertThat(
                map.extract<String>("data/actors[$i]/picture"),
                equalTo("http://picture.server/pic/$name?big=false")
            )
        }
    }

    @Test
    fun `query extension property with optional annotated argument`() {
        val map = execute("{actors{name, pictureWithArgs}}")
        for (i in 0..4) {
            val name = map.extract<String>("data/actors[$i]/name").replace(' ', '_')
            assertThat(
                map.extract<String>("data/actors[$i]/pictureWithArgs"),
                equalTo("http://picture.server/pic/$name?big=false")
            )
        }
    }

    @Test
    fun `query with mandatory generic input type`() {
        val map = execute("""{actorsByTags(tags: ["1", "2", "3"]){name}}""")
        assertNoErrors(map)
    }

    @Test
    fun `query with optional generic input type`() {
        val map = execute("{actorsByTagsOptional{name}}")
        assertNoErrors(map)
    }

    @Test
    fun `query with nulabble generic input type`() {
        val map = execute("{actorsByTagsNullable{name}}")
        assertNoErrors(map)
    }

    @Test
    fun `query with transformed property`() {
        val map = execute("{scenario{id, content(uppercase: false)}}")
        assertThat(map.extract<String>("data/scenario/content"), equalTo("Very long scenario"))

        val map2 = execute("{scenario{id, content(uppercase: true)}}")
        assertThat(map2.extract<String>("data/scenario/content"), equalTo("VERY LONG SCENARIO"))
    }

    @Test
    fun `query with invalid field arguments`() {
        invoking {
            execute("{scenario{id(uppercase: true), content}}")
        } shouldThrow ValidationException::class with {
            message shouldBeEqualTo "Property id on type Scenario has no arguments, found: [uppercase]"
        }
    }

    @Test
    fun `query with external fragment`() {
        val map = execute("{film{title, ...dir }} fragment dir on Film {director{name, age}}")
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with nested external fragment`() {
        val map = execute(
            """
            {
                film {
                    title
                    ...dir
                }
            }

            fragment dir on Film {
                director {
                    name
                }
                ...dirIntermediate
            }

            fragment dirIntermediate on Film {
                ...dirAge
            }

            fragment dirAge on Film {
                director {
                    age
                }
            }
            """.trimIndent()
        )
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with two nested external fragments`() {
        val map = execute(
            """
            {
                film {
                    ...film_All
                }
            }
            
            fragment film_All on Film {
                ...film_title
                ...film_director
            }
            
            fragment film_title on Film {
                title
            }

            fragment film_director on Film {
                director {
                    name
                }
                ...film_director_age
            }

            fragment film_director_age on Film {
                director {
                    age
                }
            }
            """.trimIndent()
        )
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with two fragments`() {
        val map = execute(
            """
            {
                film {
                    ...film_title
                    ...film_director_All
                }
            }
            
            fragment film_title on Film {
                title
            }

            fragment film_director_All on Film {
                director {
                    name
                    age
                }
            }
            """.trimIndent()
        )
        assertNoErrors(map)
        assertThat(map.extract<String>("data/film/title"), equalTo(prestige.title))
        assertThat(map.extract<String>("data/film/director/name"), equalTo(prestige.director.name))
        assertThat(map.extract<Int>("data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with missing selection set`() {
        invoking {
            execute("{film}")
        } shouldThrow GraphQLError::class withMessage "Missing selection set on property film of type Film"
    }

    data class SampleNode(val id: Int, val name: String, val fields: List<String>? = null)

    @Test
    fun `access to execution node`() {
        val result = defaultSchema {
            configure { useDefaultPrettyPrinter = true }
            query("root") {
                resolver { node: Execution.Node ->
                    SampleNode(0, "Root", fields = node.getFields())
                }
            }
            type<SampleNode> {
                property<List<SampleNode>>("children") {
                    resolver { parent, amount: Int, node: Execution.Node ->
                        (1..amount).map {
                            SampleNode(it, "${node.aliasOrKey}-Testing", fields = node.getFields())
                        }
                    }
                }
            }
        }.executeBlocking(
            """
            {
                root {
                    fields
                    kids: children(amount: 1) {
                        id
                        fields
                        ...aFragment
                    }
                }
            }
            fragment aFragment on SampleNode {
                id
                name
            }
        """.trimIndent()
        ).also(::println).deserialize()

        result.extract<List<String>>("data/root/fields") shouldBeEqualTo listOf("fields")
        result.extract<Int>("data/root/kids[0]/id") shouldBeEqualTo 1
        result.extract<String>("data/root/kids[0]/name") shouldBeEqualTo "kids-Testing"
        result.extract<List<String>>("data/root/kids[0]/fields") shouldBeEqualTo listOf("id", "fields", "name")
    }

}
