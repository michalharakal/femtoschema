package me.bechberger.util.femtoschema

import kotlin.test.*

class EnumSchemaTest {

    @Test
    fun testEnumValidation() {
        val schema = Schemas.enumOf("RED", "GREEN", "BLUE")
        assertTrue(schema.validate("RED").isValid)
        assertTrue(schema.validate("GREEN").isValid)
        assertTrue(schema.validate("BLUE").isValid)
    }

    @Test
    fun testRejectInvalidEnumValues() {
        val schema = Schemas.enumOf("ACTIVE", "INACTIVE")
        val result = schema.validate("DELETED")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("one of"))
    }

    @Test
    fun testNumericEnums() {
        val schema = Schemas.enumOf(200, 201, 400, 404, 500)
        assertTrue(schema.validate(200).isValid)
        assertTrue(schema.validate(404).isValid)
        assertFalse(schema.validate(999).isValid)
    }

    @Test
    fun testMixedTypeEnums() {
        val schema = Schemas.enumOf("success", "error", 0, 1)
        assertTrue(schema.validate("success").isValid)
        assertTrue(schema.validate(0).isValid)
        assertTrue(schema.validate(1).isValid)
        assertFalse(schema.validate("unknown").isValid)
    }

    @Test
    fun testDescription() {
        val schema = Schemas.enumOf("DRAFT", "PUBLISHED")
            .withDescription("Document status")
        assertEquals("Document status", schema.description)
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.enumOf("ACTIVE", "INACTIVE", "SUSPENDED")
        val jsonSchema = schema.toJsonSchema()
        assertTrue(jsonSchema.containsKey("enum"))
        val enumValues = jsonSchema["enum"] as List<*>
        assertEquals(3, enumValues.size)
    }

    @Test
    fun testInObjectSchema() {
        val schema = Schemas.`object`()
            .required("status", Schemas.enumOf("PENDING", "APPROVED", "REJECTED"))
            .required("priority", Schemas.enumOf("LOW", "MEDIUM", "HIGH"))

        val validData = mapOf("status" to "APPROVED", "priority" to "HIGH")
        assertTrue(schema.validate(validData).isValid)

        val invalidData = mapOf("status" to "APPROVED", "priority" to "URGENT")
        assertFalse(schema.validate(invalidData).isValid)
    }

    @Test
    fun testInArraySchema() {
        val schema = Schemas.array(Schemas.enumOf("READ", "WRITE", "EXECUTE"))

        val validData = listOf("READ", "WRITE", "EXECUTE")
        assertTrue(schema.validate(validData).isValid)

        val invalidData = listOf("READ", "WRITE", "DELETE")
        assertFalse(schema.validate(invalidData).isValid)
    }
}
