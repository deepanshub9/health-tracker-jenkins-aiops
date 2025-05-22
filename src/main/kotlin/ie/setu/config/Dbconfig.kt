package ie.setu.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.postgresql.util.PSQLException

class DbConfig {

    private val logger = KotlinLogging.logger {}
    private lateinit var dbConfig: Database

    fun getDbConnection(): Database {
        val dbType = System.getenv("DB_TYPE") ?: "postgres"  // default to postgres

        return try {
            if (dbType == "h2") {
                logger.info { "Connecting to H2 in-memory database for tests..." }
                dbConfig = Database.connect(
                    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
                    driver = "org.h2.Driver",
                    user = "root",
                    password = ""
                )
            } else {
                val PGHOST = System.getenv("PGHOST") ?: "dpg-cvc253jtq21c73e5dd50-a.frankfurt-postgres.render.com"
                val PGPORT = System.getenv("PGPORT") ?: "5432"
                val PGUSER = System.getenv("PGUSER") ?: "health_tracker_gkwy_user"
                val PGPASSWORD = System.getenv("PGPASSWORD") ?: "xxPJzKaqaAkB191XVZGghL7oytS5Q0Jh"
                val PGDATABASE = System.getenv("PGDATABASE") ?: "health_tracker_gkwy"

                val dbUrl = "jdbc:postgresql://$PGHOST:$PGPORT/$PGDATABASE"
                logger.info { "Connecting to PostgreSQL... $dbUrl" }

                dbConfig = Database.connect(
                    url = dbUrl,
                    driver = "org.postgresql.Driver",
                    user = PGUSER,
                    password = PGPASSWORD
                )
            }
            logger.info { "Database connected successfully." }
            dbConfig
        } catch (e: PSQLException) {
            logger.error(e) { "Error during DB connection!" }
            throw e
        }
    }
}
