package com.apurebase.kgraphql.model

val users = listOf(
        User(id = 1, name = "Amber"),
        User(id = 2, name = "Greg"),
        User(id = 3, name = "Frank"))

data class User(val id: Int = -1, val name: String = "")