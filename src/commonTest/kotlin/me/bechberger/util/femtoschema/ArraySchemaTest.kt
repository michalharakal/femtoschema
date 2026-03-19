package me.bechberger.util.femtoschema

import kotlin.test.*

class ArraySchemaTest {

    @Test
    fun testArrayOfStrings() {
        val schema = Schemas.array(Schemas.string())
        assertTrue(schema.validate(listOf<String>()).isValid)
        assertTrue(schema.validate(listOf("a", "b", "c")).isValid)
        val result = schema.validate(listOf("a", 123, "c"))
        assertFalse(result.isValid)
    }

    @Test
    fun testArrayOfNumbers() {
        val schema = Schemas.array(Schemas.number())
        assertTrue(schema.validate(listOf<Int>()).isValid)
        assertTrue(schema.validate(listOf(1, 2.5, 3)).isValid)
        val result = schema.validate(listOf(1, "two", 3))
        assertFalse(result.isValid)
    }

    @Test
    fun testMinimumItems() {
        val schema = Schemas.array(Schemas.string()).withMinItems(2)
        assertTrue(schema.validate(listOf("a", "b")).isValid)
        val result = schema.validate(listOf("a"))
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("too few"))
    }

    @Test
    fun testMaximumItems() {
        val schema = Schemas.array(Schemas.string()).withMaxItems(2)
        assertTrue(schema.validate(listOf("a", "b")).isValid)
        val result = schema.validate(listOf("a", "b", "c"))
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("too many"))
    }

    @Test
    fun testUniqueItems() {
        val schema = Schemas.array(Schemas.string()).withUniqueItems(true)
        assertTrue(schema.validate(listOf("a", "b", "c")).isValid)
        val result = schema.validate(listOf("a", "b", "a"))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("unique") })
    }

    @Test
    fun testArrayOfObjects() {
        val personSchema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())

        val schema = Schemas.array(personSchema)

        val validData = listOf(
            mapOf("name" to "Alice", "age" to 30.0),
            mapOf("name" to "Bob", "age" to 25.0)
        )
        assertTrue(schema.validate(validData).isValid)

        val invalidData = listOf(
            mapOf("name" to "Alice", "age" to 30.0),
            mapOf("name" to "Bob", "age" to "invalid")
        )
        assertFalse(schema.validate(invalidData).isValid)
    }

    @Test
    fun testRejectNonArrays() {
        val schema = Schemas.array(Schemas.string())
        val result = schema.validate("not an array")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("Expected array"))
    }

    @Test
    fun testArrayItemErrorPaths() {
        val schema = Schemas.array(Schemas.number().withMinimum(0.0))
        val result = schema.validate(listOf(10, -5, 20))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.path.contains("[1]") })
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.array(Schemas.string())
            .withMinItems(1)
            .withMaxItems(10)

        val jsonSchema = schema.toJsonSchema()
        assertEquals("array", jsonSchema["type"])
        assertEquals(1, jsonSchema["minItems"])
        assertEquals(10, jsonSchema["maxItems"])
    }
}
