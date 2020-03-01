package com.apurebase.kgraphql.dao

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CSVDataImporter {
    fun importFromCsv(inputStream: InputStream?, consumer: (Array<String>)-> Unit) {
        inputStream ?: throw RuntimeException("Failed to perform import")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        try {
            return bufferedReader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    consumer(line.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                }
            }
        } catch (e: Exception) {
            println(e)
            throw RuntimeException("Failed to perform import", e)
        }
    }

}