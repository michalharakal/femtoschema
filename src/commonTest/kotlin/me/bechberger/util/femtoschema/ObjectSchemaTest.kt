package me.bechberger.util.femtoschema

import kotlin.test.*

class ObjectSchemaTest {

    @Test
    fun testSimpleObjectValidation() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())

        val data = mapOf("name" to "Alice", "age" to 30.0)
        assertTrue(schema.validate(data).isValid)
    }

    @Test
    fun testRejectNonObjects() {
        val schema = Schemas.`object`()
        val result = schema.validate("not an object")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("Expected object"))
    }

    @Test
    fun testRequiredProperties() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())

        val resultMissingName = schema.validate(mapOf("age" to 30.0))
        assertFalse(resultMissingName.isValid)
        assertTrue(resultMissingName.errors.any { it.path.contains("name") })

        val resultMissingAge = schema.validate(mapOf("name" to "Bob"))
        assertFalse(resultMissingAge.isValid)
        assertTrue(resultMissingAge.errors.any { it.path.contains("age") })
    }

    @Test
    fun testOptionalProperties() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .optional("email", Schemas.string())

        val withEmail = mapOf("name" to "Alice", "email" to "alice@example.com")
        assertTrue(schema.validate(withEmail).isValid)

        val withoutEmail = mapOf("name" to "Bob")
        assertTrue(schema.validate(withoutEmail).isValid)
    }

    @Test
    fun testPropertyTypeValidation() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())

        val wrongTypes = mapOf("name" to "Alice", "age" to "thirty")
        val result = schema.validate(wrongTypes)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path.contains("age") })
    }

    @Test
    fun testAdditionalPropertiesAllowed() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())

        val dataWithExtra = mapOf("name" to "Alice", "extra" to "field")
        assertTrue(schema.validate(dataWithExtra).isValid)
    }

    @Test
    fun testDisallowAdditionalProperties() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .allowAdditionalProperties(false)

        val dataWithExtra = mapOf("name" to "Alice", "extra" to "field")
        val result = schema.validate(dataWithExtra)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Additional properties") })
    }

    @Test
    fun testNestedObjects() {
        val addressSchema = Schemas.`object`()
            .required("street", Schemas.string())
            .required("city", Schemas.string())

        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .optional("address", addressSchema)

        val validData = mapOf(
            "name" to "Alice",
            "address" to mapOf("street" to "Main St", "city" to "Springfield")
        )
        assertTrue(schema.validate(validData).isValid)

        val invalidAddress = mapOf(
            "name" to "Bob",
            "address" to mapOf("street" to "Main St", "city" to 123)
        )
        assertFalse(schema.validate(invalidAddress).isValid)
    }

    @Test
    fun testNestedErrorPaths() {
        val schema = Schemas.`object`()
            .required("user", Schemas.`object`()
                .required("name", Schemas.string())
                .required("age", Schemas.number().withMinimum(0.0)))

        val invalidData = mapOf(
            "user" to mapOf("name" to "Alice", "age" to -5.0)
        )

        val result = schema.validate(invalidData)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path.contains("$.user.age") })
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())
            .optional("email", Schemas.string())

        val jsonSchema = schema.toJsonSchema()
        assertEquals("object", jsonSchema["type"])
        assertTrue(jsonSchema.containsKey("properties"))
        assertTrue(jsonSchema.containsKey("required"))
    }
}
