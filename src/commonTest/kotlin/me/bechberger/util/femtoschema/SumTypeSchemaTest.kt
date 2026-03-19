package me.bechberger.util.femtoschema

import kotlin.test.*

class SumTypeSchemaTest {

    @Test
    fun testBasicSumTypeValidation() {
        val emailVariant = Schemas.`object`()
            .required("type", Schemas.enumOf("email"))
            .required("address", Schemas.string())

        val smsVariant = Schemas.`object`()
            .required("type", Schemas.enumOf("sms"))
            .required("phoneNumber", Schemas.string())

        val schema = Schemas.sumType("type")
            .variant("email", emailVariant)
            .variant("sms", smsVariant)

        val emailData = mapOf("type" to "email", "address" to "user@example.com")
        assertTrue(schema.validate(emailData).isValid)

        val smsData = mapOf("type" to "sms", "phoneNumber" to "+1234567890")
        assertTrue(schema.validate(smsData).isValid)
    }

    @Test
    fun testMissingDiscriminator() {
        val schema = Schemas.sumType("type")
            .variant("a", Schemas.`object`().required("type", Schemas.enumOf("a")))
            .variant("b", Schemas.`object`().required("type", Schemas.enumOf("b")))

        val data = mapOf("data" to "value")
        val result = schema.validate(data)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Discriminator field") })
    }

    @Test
    fun testUnknownDiscriminatorValue() {
        val schema = Schemas.sumType("type")
            .variant("email", Schemas.`object`().required("type", Schemas.enumOf("email")))
            .variant("sms", Schemas.`object`().required("type", Schemas.enumOf("sms")))

        val data = mapOf("type" to "slack")
        val result = schema.validate(data)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Unknown discriminator value") })
    }

    @Test
    fun testVariantSchemaValidation() {
        val variant1 = Schemas.`object`()
            .required("type", Schemas.enumOf("v1"))
            .required("field1", Schemas.string())

        val variant2 = Schemas.`object`()
            .required("type", Schemas.enumOf("v2"))
            .required("field2", Schemas.number().withMinimum(0.0))

        val schema = Schemas.sumType("type")
            .variant("v1", variant1)
            .variant("v2", variant2)

        assertTrue(schema.validate(mapOf("type" to "v1", "field1" to "value")).isValid)
        assertFalse(schema.validate(mapOf("type" to "v1", "field1" to 123)).isValid)
        assertFalse(schema.validate(mapOf("type" to "v2", "field2" to -5.0)).isValid)
    }

    @Test
    fun testMultipleVariants() {
        val successVariant = Schemas.`object`()
            .required("status", Schemas.enumOf("success"))
            .required("data", Schemas.string())

        val errorVariant = Schemas.`object`()
            .required("status", Schemas.enumOf("error"))
            .required("error", Schemas.string())

        val warningVariant = Schemas.`object`()
            .required("status", Schemas.enumOf("warning"))
            .required("warning", Schemas.string())

        val schema = Schemas.sumType("status")
            .variant("success", successVariant)
            .variant("error", errorVariant)
            .variant("warning", warningVariant)

        assertTrue(schema.validate(mapOf("status" to "success", "data" to "ok")).isValid)
        assertTrue(schema.validate(mapOf("status" to "error", "error" to "failed")).isValid)
        assertTrue(schema.validate(mapOf("status" to "warning", "warning" to "deprecated")).isValid)
    }

    @Test
    fun testComplexSumType() {
        val userCreatedEvent = Schemas.`object`()
            .required("eventType", Schemas.enumOf("user.created"))
            .required("userId", Schemas.string())
            .required("metadata", Schemas.`object`()
                .required("email", Schemas.string())
                .required("createdAt", Schemas.string()))

        val userDeletedEvent = Schemas.`object`()
            .required("eventType", Schemas.enumOf("user.deleted"))
            .required("userId", Schemas.string())

        val schema = Schemas.sumType("eventType")
            .variant("user.created", userCreatedEvent)
            .variant("user.deleted", userDeletedEvent)

        val createEvent = mapOf(
            "eventType" to "user.created",
            "userId" to "123",
            "metadata" to mapOf("email" to "user@example.com", "createdAt" to "2026-02-24T10:00:00Z")
        )
        assertTrue(schema.validate(createEvent).isValid)

        val deleteEvent = mapOf("eventType" to "user.deleted", "userId" to "123")
        assertTrue(schema.validate(deleteEvent).isValid)
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.sumType("type")
            .variant("a", Schemas.`object`().required("type", Schemas.enumOf("a")))
            .variant("b", Schemas.`object`().required("type", Schemas.enumOf("b")))

        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema.containsKey("oneOf"))
        assertTrue(jsonSchema.containsKey("discriminator"))
    }

    @Test
    fun testDescription() {
        val schema = Schemas.sumType("type")
            .withDescription("Payment method selector")
        assertEquals("Payment method selector", schema.description)
    }
}
