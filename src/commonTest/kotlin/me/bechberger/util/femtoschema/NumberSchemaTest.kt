package me.bechberger.util.femtoschema

import kotlin.test.*

class NumberSchemaTest {

    @Test
    fun testNumberValidation() {
        val schema = Schemas.number()
        assertTrue(schema.validate(42).isValid)
        assertTrue(schema.validate(3.14).isValid)
        assertTrue(schema.validate(0).isValid)
        assertTrue(schema.validate(-100).isValid)
        assertTrue(schema.validate(1L).isValid)
    }

    @Test
    fun testRejectNonNumbers() {
        val schema = Schemas.number()
        val result = schema.validate("42")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("Expected number"))
    }

    @Test
    fun testMinimumValue() {
        val schema = Schemas.number().withMinimum(0.0)
        assertTrue(schema.validate(0).isValid)
        assertTrue(schema.validate(100).isValid)
        val result = schema.validate(-1)
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("less than minimum"))
    }

    @Test
    fun testMaximumValue() {
        val schema = Schemas.number().withMaximum(100.0)
        assertTrue(schema.validate(100).isValid)
        assertTrue(schema.validate(50).isValid)
        val result = schema.validate(101)
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("greater than maximum"))
    }

    @Test
    fun testMinMaxValues() {
        val schema = Schemas.number().withMinimum(10.0).withMaximum(100.0)
        assertTrue(schema.validate(10).isValid)
        assertTrue(schema.validate(50).isValid)
        assertTrue(schema.validate(100).isValid)
        assertFalse(schema.validate(9).isValid)
        assertFalse(schema.validate(101).isValid)
    }

    @Test
    fun testExclusiveMinimum() {
        val schema = Schemas.number().withExclusiveMinimum(0.0)
        assertTrue(schema.validate(0.1).isValid)
        assertTrue(schema.validate(100).isValid)
        val result = schema.validate(0)
        assertFalse(result.isValid)
    }

    @Test
    fun testExclusiveMaximum() {
        val schema = Schemas.number().withExclusiveMaximum(100.0)
        assertTrue(schema.validate(99.9).isValid)
        assertTrue(schema.validate(0).isValid)
        val result = schema.validate(100)
        assertFalse(result.isValid)
    }

    @Test
    fun testDescription() {
        val schema = Schemas.number().withDescription("User age")
        assertEquals("User age", schema.description)
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.number()
            .withMinimum(0.0)
            .withMaximum(100.0)

        val jsonSchema = schema.toJsonSchema()
        assertEquals("number", jsonSchema["type"])
        assertEquals(0.0, jsonSchema["minimum"])
        assertEquals(100.0, jsonSchema["maximum"])
    }

    @Test
    fun testFloatingPointPrecision() {
        val schema = Schemas.number().withMinimum(0.5).withMaximum(10.5)
        assertTrue(schema.validate(0.5).isValid)
        assertTrue(schema.validate(5.5).isValid)
        assertTrue(schema.validate(10.5).isValid)
        assertFalse(schema.validate(0.4).isValid)
        assertFalse(schema.validate(10.6).isValid)
    }
}
