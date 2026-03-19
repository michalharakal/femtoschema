package me.bechberger.util.femtoschema

import kotlin.test.*

class StringSchemaTest {

    @Test
    fun testSimpleStringValidation() {
        val schema = Schemas.string()
        assertTrue(schema.validate("hello").isValid)
        assertTrue(schema.validate("").isValid)
        assertTrue(schema.validate("with spaces and 123").isValid)
    }

    @Test
    fun testRejectNonStringValues() {
        val schema = Schemas.string()
        val result = schema.validate(123)
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].message.contains("Expected string"))
    }

    @Test
    fun testRejectNullValues() {
        val schema = Schemas.string()
        val result = schema.validate(null)
        assertFalse(result.isValid)
    }

    @Test
    fun testMinimumLength() {
        val schema = Schemas.string().withMinLength(5)
        assertTrue(schema.validate("hello").isValid)
        assertTrue(schema.validate("hello world").isValid)
        val result = schema.validate("hi")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("too short"))
    }

    @Test
    fun testMaximumLength() {
        val schema = Schemas.string().withMaxLength(10)
        assertTrue(schema.validate("hello").isValid)
        assertTrue(schema.validate("0123456789").isValid)
        val result = schema.validate("01234567890")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("too long"))
    }

    @Test
    fun testMinMaxLength() {
        val schema = Schemas.string().withMinLength(3).withMaxLength(10)
        assertTrue(schema.validate("abc").isValid)
        assertTrue(schema.validate("1234567890").isValid)
        assertFalse(schema.validate("ab").isValid)
        assertFalse(schema.validate("12345678901").isValid)
    }

    @Test
    fun testStringPattern() {
        val emailSchema = Schemas.string()
            .withPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        assertTrue(emailSchema.validate("user@example.com").isValid)
        assertTrue(emailSchema.validate("test.email+tag@domain.co.uk").isValid)
        val result = emailSchema.validate("invalid-email")
        assertFalse(result.isValid)
        assertTrue(result.errors[0].message.contains("pattern"))
    }

    @Test
    fun testDescription() {
        val schema = Schemas.string().withDescription("User's email address")
        assertEquals("User's email address", schema.description)
    }

    @Test
    fun testJsonSchemaExport() {
        val schema = Schemas.string()
            .withMinLength(5)
            .withMaxLength(100)
            .withPattern("[A-Z].*")

        val jsonSchema = schema.toJsonSchema()
        assertEquals("string", jsonSchema["type"])
        assertEquals(5, jsonSchema["minLength"])
        assertEquals(100, jsonSchema["maxLength"])
        assertEquals("[A-Z].*", jsonSchema["pattern"])
    }

    @Test
    fun testBuilderChaining() {
        val schema = Schemas.string()
            .withMinLength(1)
            .withMaxLength(50)
            .withPattern("[a-z]+")
            .withDescription("Lowercase string")
        assertTrue(schema.validate("abc").isValid)
        assertFalse(schema.validate("ABC").isValid)
    }
}
