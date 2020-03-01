package com.apurebase.kgraphql.dao

import com.apurebase.kgraphql.dao.CSVDataImporter.importFromCsv
import com.apurebase.kgraphql.model.UFOSightings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DatabaseFactory {

    private fun Thread.asResourceStream(): InputStream? = this.contextClassLoader.getResourceAsStream(CSV_FILE_NAME)

    private const val CSV_FILE_NAME = "ufo_sightings_2013_2014"
    private const val DATE_FORMAT = "M/d/yyyy"

    fun init() {
        Database.connect(hikari())
        transaction {
            create(UFOSightings)
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
            val settingsStream = Thread.currentThread().asResourceStream()
            importFromCsv(settingsStream) { row ->
                UFOSightings.insert {
                    it[dateSighting] = LocalDate.parse(row[0], formatter)
                    it[city] = row[1]
                    it[state] = row[2]
                    it[country] = row[3]
                    it[shape] = row[4]
                    it[duration] = row[5].toDouble()
                    it[comments] = row[6]
                    it[latitude] = row[7].toDouble()
                    it[longitude] = row[8].toDouble()
                }
            }
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(
            block: suspend () -> T): T =
            newSuspendedTransaction { block() }


}



fun <T:Any> String.execAndMap(transform : (ResultSet) -> T) : MutableList<T> {
    val result = arrayListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}