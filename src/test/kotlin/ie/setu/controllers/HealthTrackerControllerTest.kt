package ie.setu.controllers

import ie.setu.config.JavalinConfig
import ie.setu.domain.User
import ie.setu.helpers.*
import ie.setu.utils.jsonToObject
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthTrackerControllerTest {

    private val app = JavalinConfig().startJavalinService()
    private val origin = "http://localhost:8001"

    @AfterAll
    fun tearDown() {
        app.stop()
    }

    @Nested
    inner class ReadUsers {
        @Test
        fun `get all users returns 200 and list or 404 if none`() {
            val response = Unirest.get("$origin/api/users").asString()
            if (response.status == 200) {
                val retrievedUsers: List<User> = jsonToObject(response.body.toString())
                assertTrue(retrievedUsers.isNotEmpty(), "Expected non-empty list of users")
            } else {
                assertEquals(404, response.status)
            }
        }

        @Test
        fun `get user by id returns 200 if exists, 404 if not`() {
            val addResponse = addUser("Test User", "testuser@email.com")
            assertEquals(201, addResponse.status)
            val addedUser: User = jsonToObject(addResponse.body.toString())
            val response = Unirest.get("$origin/api/users/${addedUser.id}").asString()
            assertEquals(200, response.status)
            val retrievedUser: User = jsonToObject(response.body.toString())
            assertEquals(addedUser.id, retrievedUser.id)
            assertEquals("Test User", retrievedUser.name)
            val notFoundResponse = Unirest.get("$origin/api/users/99999").asString()
            assertEquals(404, notFoundResponse.status)
            deleteUser(addedUser.id)
        }

        @Test
        fun `get user by email returns 200 if exists, 404 if not`() {
            val addResponse = addUser("Email User", "unique@email.com")
            assertEquals(201, addResponse.status)
            val response = Unirest.get("$origin/api/users/email/unique@email.com").asString()
            assertEquals(200, response.status)
            val retrievedUser: User = jsonToObject(response.body.toString())
            assertEquals("Email User", retrievedUser.name)
            val notFoundResponse = Unirest.get("$origin/api/users/email/notfound@email.com").asString()
            assertEquals(404, notFoundResponse.status)
            val addedUser: User = jsonToObject(addResponse.body.toString())
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class CreateUsers {
        @Test
        fun `add a user returns 201 and user is retrievable`() {
            val addResponse = addUser("Integration User", "integration@email.com")
            assertEquals(201, addResponse.status)
            val addedUser: User = jsonToObject(addResponse.body.toString())
            val getResponse = Unirest.get("$origin/api/users/${addedUser.id}").asString()
            assertEquals(200, getResponse.status)
            val retrievedUser: User = jsonToObject(getResponse.body.toString())
            assertEquals("Integration User", retrievedUser.name)
            assertEquals("integration@email.com", retrievedUser.email)
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class UpdateUsers {
        @Test
        fun `update an existing user returns 204 and updates user`() {
            val addResponse = addUser("Old Name", "old@email.com")
            assertEquals(201, addResponse.status)
            val addedUser: User = jsonToObject(addResponse.body.toString())
            val updateResponse = updateUser(addedUser.id, "New Name", "new@email.com")
            assertEquals(204, updateResponse.status)
            val getResponse = Unirest.get("$origin/api/users/${addedUser.id}").asString()
            val updatedUser: User = jsonToObject(getResponse.body.toString())
            assertEquals("New Name", updatedUser.name)
            assertEquals("new@email.com", updatedUser.email)
            deleteUser(addedUser.id)
        }

        @Test
        fun `update a non-existing user returns 404`() {
            val updateResponse = updateUser(99999, "No Name", "no@email.com")
            assertEquals(404, updateResponse.status)
        }
    }

    @Nested
    inner class DeleteUsers {
        @Test
        fun `delete a user returns 204 if exists, 404 if not`() {
            val addResponse = addUser("Delete User", "delete@email.com")
            assertEquals(201, addResponse.status)
            val addedUser: User = jsonToObject(addResponse.body.toString())
            val deleteResponse = deleteUser(addedUser.id)
            assertEquals(204, deleteResponse.status)
            val deleteAgainResponse = deleteUser(addedUser.id)
            assertEquals(404, deleteAgainResponse.status)
        }
    }

    // Helper functions
    private fun addUser(name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }

    private fun updateUser(id: Int, name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.patch("$origin/api/users/$id")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }

    private fun deleteUser(id: Int): HttpResponse<String> {
        return Unirest.delete("$origin/api/users/$id").asString()
    }
}