package ie.setu.controllers

import ie.setu.config.DbConfig
import ie.setu.domain.Activity
import ie.setu.domain.User
import ie.setu.helpers.*
import ie.setu.utils.jsonToObject
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class ActivityControllerTest {

    private val db = DbConfig().getDbConnection()
    private val app = ServerContainer.instance
    private val origin = "http://localhost:" + app.port()

    @Nested
    inner class ReadActivity {
        @Test
        fun `get all activities from the database returns 200 or 404 response`() {
            val response = Unirest.get(origin + "/api/activities/").asString()
            if (response.status == 200) {
                val retrievedactivities: ArrayList<Activity> = jsonToObject(response.body.toString())
                assertNotEquals(0, retrievedactivities.size)
            }
            else {
                assertEquals(404, response.status)
            }
        }
        @Test
        fun `get activities by id when user does not exist returns 404 response`() {

            //Arrange - test data for user id
            val id = -1

            // Act - attempt to retrieve the non-existent user from the database
            val retrieveResponse = Unirest.get(origin + "/api/activities/${id}").asString()

            // Assert -  verify return code
            assertEquals(400, retrieveResponse.status)
        }
    }
    @Nested
    inner class CreateActivities{
        @Test
        fun `add a activity with correct details returns a 201 response`(){
            //Arrange - add the user
            val addResponse = addUser(users[0].name,"testr296@gmail.com")
            val useradd = addResponse.body.toString()
            val addedUser : User = jsonToObject(addResponse.body.toString())
            val addActivityResponse = addActivity(activities[0].description,activities[0].duration,activities[0].calories,activities[0].started,addedUser.id
            )
            assertEquals(201, addActivityResponse.status)
            deleteUser(addedUser.id)
        }
    }
    @Nested
    inner class UpdateActivity{
        @Test
        fun `updating a activity when it exists, returns a 200 response`(){
            val addResponse = addUser(users[0].name,"testj894@gmail.com")
            val useradd = addResponse.body.toString()
            val addedUser : User = jsonToObject(addResponse.body.toString())
            val addActivityResponse = addActivity(activities[0].description,activities[0].duration,activities[0].calories,activities[0].started,addedUser.id
            )
            assertEquals(201, addActivityResponse.status)
            val addedActivity:Activity = jsonToObject(addActivityResponse.body.toString())
            val activityId = addedActivity.id
            val updatedActivityResponse = updateActivity(activityId, "dancing", 60.0, activities[0].calories,activities[0].started,addedUser.id)
            assertEquals(200, updatedActivityResponse.status)
            val retrievedActivityResponse = retrieveActivityById(activityId)
            val updatedActivity:Activity = jsonToObject(retrievedActivityResponse.body.toString())
            assertEquals("dancing", updatedActivity.description)
            assertEquals(60.0, updatedActivity.duration)
        }
        @Test
        fun `updating a activity when it doesn't exist, returns a 404 response`() {

            //Act & Assert - attempt to update the email and name of user that doesn't exist
            assertEquals(404, updateActivity(-1, activities[0].description,activities[0].duration,activities[0].calories,activities[0].started,-1).status)
        }
    }
    @Nested
    inner class DeleteActivities{
        @Test
        fun `deleting a activity when it doesn't exist, returns a 404 response`() {
            //Act & Assert - attempt to delete a user that doesn't exist
            assertEquals(404, deleteActivity(-1).status)
        }
        @Test
        fun `deleting a activity when it exists, returns a 204 response`(){
            val addResponse = addUser(users[0].name,"testi210@gmail.com")
            val useradd = addResponse.body.toString()
            val addedUser : User = jsonToObject(addResponse.body.toString())
            val addActivityResponse = addActivity(activities[0].description,activities[0].duration,activities[0].calories,activities[0].started,addedUser.id
            )
            val addedActivity:Activity = jsonToObject(addActivityResponse.body.toString())
            val activityId = addedActivity.id
            assertEquals(204, deleteActivity(activityId).status)
        }
    }




    //helper function to add a test user to the database
    private fun addUser (name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }
    private fun addActivity(
                             description: String,
                             duration: Double,
                             calories: Int,
                             started: DateTime,
                             userId: Int
    ): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/activities")
            .body("{\"description\":\"$description\", \"duration\":\"$duration\", \"calories\":\"$calories\", \"started\":\"$started\", \"userId\":\"$userId\" }")
            .asJson()
    }
    private fun retrieveActivityById(id: Int) : HttpResponse<JsonNode> {
        return Unirest.get(origin+"/api/activities/$id").asJson()
    }

    //helper function to delete a test user from the database
    private fun deleteUser (id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/users/$id").asString()
    }
    private fun updateActivity(
        id: Int,
        description: String,
        duration: Double,
        calories: Int,
        started: DateTime,
        userId: Int
    ): HttpResponse<JsonNode> {
        return Unirest.patch(origin + "/api/activities/$id")
            .body("{\"description\":\"$description\", \"duration\":\"$duration\", \"calories\":\"$calories\", \"started\":\"$started\", \"userId\":\"$userId\" }")
            .asJson()
    }
    //helper function to delete a test user from the database
    private fun deleteActivity (id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/activities/$id").asString()
    }



}






























































