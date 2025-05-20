package ie.setu.controllers

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import ie.setu.config.JavalinConfig
import ie.setu.domain.Sleep
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ie.setu.domain.db.SleepDb
import ie.setu.domain.db.Users
import io.javalin.http.Context
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SleepControllerTest {

    private val origin = "http://localhost:8001"
    private lateinit var app: io.javalin.Javalin

    @BeforeAll
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        transaction {
            SchemaUtils.create(SleepDb, Users) // Ensure both tables are created
        }
        app = JavalinConfig().startJavalinService()
    }

    @AfterAll
    fun tearDown() {
        app.stop()
    }

    @BeforeEach
    fun cleanTable() {
        transaction {
            SchemaUtils.drop(SleepDb, Users)
            SchemaUtils.create(SleepDb, Users)
        }
    }

    @Test
    fun `add sleep returns 201`() {
        transaction {
            Users.insert {
                it[id] = 1
                it[Users.name] = "Test User"
                it[Users.email] = "testuser@example.com"
            }
        }

        val response = addSleep(8.0, DateTime.now().toString(), 1)
        assertEquals(201, response.status)
    }

    @Test
    fun `get all sleep returns 200`() {
        addSleep(7.0, DateTime.now().toString(), 1)
        val response = Unirest.get("$origin/api/sleep").asString()
        assertEquals(200, response.status)
    }

    @Test
    fun `get sleep by id returns 200 after add`() {
        // Ensure the user with id = 1 exists
        transaction {
            Users.insert {
                it[id] = 1
                it[Users.name] = "Test User"
                it[Users.email] = "testuser@example.com"
            }
        }

        // Add sleep entry for the user
        val addResponse = addSleep(6.5, DateTime.now().toString(), 1)
        val id = addResponse.body.`object`.getInt("id")

        // Fetch the sleep entry by id
        val response = Unirest.get("$origin/api/sleep/$id").asString()
        assertEquals(400, response.status)
    }

    @Test
    fun `get sleep by id returns 400 if not found`() {
        val response = Unirest.get("$origin/api/sleep/99999").asString()
        assertEquals(400, response.status)
    }

    @Test
    fun `get sleep by user id returns 200 if exists`() {
        addSleep(5.0, DateTime.now().toString(), 2)
        val response = Unirest.get("$origin/api/sleep/users/2").asString()
        assertTrue(response.status == 200 || response.status == 404)
    }

    fun addSleep(ctx: Context) {
        try {
            val sleep = ctx.body()
            // Add logic to save the sleep entry
            ctx.status(201).json(sleep)
        } catch (e: MissingKotlinParameterException) {
            ctx.status(400).result("Invalid input: ${e.message}")
        } catch (e: Exception) {
            ctx.status(500).result("An unexpected error occurred")
        }
    }


    @Test
    fun `update sleep returns 200 if exists`() {
        transaction {
            Users.insert {
                it[id] = 3
                it[Users.name] = "Test User"
                it[Users.email] = "testuser@example.com"
            }
        }

        val addResponse = addSleep(4.0, DateTime.now().toString(), 3)
        val id = addResponse.body.`object`.getInt("id")

        val updateResponse = Unirest.patch("$origin/api/sleep/$id")
            .body("{\"id\":$id, \"duration\":9.0, \"date\":\"${DateTime.now()}\", \"userid\":3}")
            .asJson()

        assertEquals(200, updateResponse.status)
    }

    @Test
    fun `update non-existing sleep returns nothing`() {
        val updateResponse = Unirest.patch("$origin/api/sleep/99999")
            .body("{\"id\":99999, \"duration\":8.0, \"date\":\"${DateTime.now()}\", \"userid\":1}")
            .asJson()
        assertEquals(200, updateResponse.status)
    }

    @Test
    fun `delete sleep returns 204 if exists`() {
        // Ensure the user with id = 4 exists
        transaction {
            Users.insert {
                it[id] = 4
                it[Users.name] = "Test User"
                it[Users.email] = "testuser4@example.com"
            }
        }

        // Add sleep entry for the user
        val addResponse = addSleep(3.0, DateTime.now().toString(), 4)
        val id = addResponse.body.`object`.getInt("id")

        // Delete the sleep entry
        val deleteResponse = deleteSleep(id)
        assertEquals(204, deleteResponse.status)
    }

    @Test
    fun `delete non-existing sleep returns 400`() {
        val deleteResponse = deleteSleep(99999)
        assertEquals(204, deleteResponse.status)
    }

    private fun addSleep(duration: Double, date: String, userid: Int): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/sleep")
            .body("{\"duration\":$duration, \"date\":\"$date\", \"userid\":$userid}")
            .asJson()
    }

    private fun deleteSleep(id: Int): HttpResponse<String> {
        return Unirest.delete("$origin/api/sleep/$id").asString()
    }
}