package io.pleo.antaeus

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.oneOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("black-box")
class InvoiceResourceTest {

    @BeforeAll
    fun setupEnv() {
        SystemTestEnvironment.start()
    }

    @Test
    fun `should be able to successfully fetch invoices`() {
        given()
            .baseUri(SystemTestEnvironment.serviceUrl)
            .`when`()
            .get("/v1/invoices")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0))
    }

    @Test
    fun `should get proper response for one particular invoice`() {
        given()
            .baseUri(SystemTestEnvironment.serviceUrl)
            .`when`()
            .get("/v1/invoices/1")
            .then()
            .statusCode(200)
            .body("id", equalTo(1))
            .body("customerId", equalTo(1))
            .body("amount.currency", `is`(oneOf("EUR", "USD", "DKK", "SEK", "GBP")))
            .body("amount.value.toBigDecimal()", `is`(closeTo(BigDecimal("255.00"), BigDecimal("245.00"))))
            .body("status", `is`(oneOf("PENDING", "PAID", "UNPAID")))
    }

}