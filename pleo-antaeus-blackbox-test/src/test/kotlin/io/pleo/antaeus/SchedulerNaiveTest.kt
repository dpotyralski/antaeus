package io.pleo.antaeus

import io.restassured.RestAssured
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.shaded.org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("black-box")
class SchedulerNaiveTest {

    @BeforeAll
    fun setupEnv() {
        SystemTestEnvironment.start()
    }

    @Test
    fun `should eventually have all invoices paid or unpaid`() {
        Awaitility.await().atMost(60, TimeUnit.SECONDS).untilAsserted {
            RestAssured.given()
                .baseUri(SystemTestEnvironment.serviceUrl)
                .`when`()
                .get("/v1/invoices")
                .then()
                .statusCode(200)
                .body("findAll { it }.status", anyOf(hasItem("PAID"), hasItem("UNPAID"), hasItems("PAID", "UNPAID")))
        }
    }

}