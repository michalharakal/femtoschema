package me.bechberger.util.femtoschema

import kotlin.test.*

class BooleanSchemaTest {

    @Test
    fun testBooleanValidation() {
        val schema = Schemas.bool()
        assertTrue(schema.validate(true).isValid)
        assertTrue(schema.validate(false).isValid)
    }

    @Test
    fun testRejectNonBooleans() {
        val schema = Schemas.bool()
        assertFalse(schema.validate("true").isValid)
        assertFalse(schema.validate(1).isValid)
        assertFalse(schema.validate(null).isValid)
    }

    @Test
    fun testDescription() {
        val schema = Schemas.bool().withDescription("Feature flag")
        assertEquals("Feature flag", schema.description)
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.bool()
        val jsonSchema = schema.toJsonSchema()
        assertEquals("boolean", jsonSchema["type"])
    }

    @Test
    fun testInObjectSchema() {
        val schema = Schemas.`object`()
            .required("active", Schemas.bool())
            .required("premium", Schemas.bool())

        val validData = mapOf("active" to true, "premium" to false)
        assertTrue(schema.validate(validData).isValid)

        val invalidData = mapOf("active" to "yes", "premium" to true)
        assertFalse(schema.validate(invalidData).isValid)
    }
}
