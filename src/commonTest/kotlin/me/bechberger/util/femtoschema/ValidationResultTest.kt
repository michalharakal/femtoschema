package me.bechberger.util.femtoschema

import kotlin.test.*

class ValidationResultTest {

    @Test
    fun testValidResult() {
        val result = ValidationResult.valid()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testInvalidResult() {
        val result = ValidationResult.invalid("$.field", "Invalid value")
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals("$.field", result.errors[0].path)
        assertEquals("Invalid value", result.errors[0].message)
    }

    @Test
    fun testMultipleErrors() {
        val errors = listOf(
            ValidationError("$.field1", "Error 1"),
            ValidationError("$.field2", "Error 2"),
            ValidationError("$.field3", "Error 3")
        )
        val result = ValidationResult.invalid(errors)
        assertFalse(result.isValid)
        assertEquals(3, result.errors.size)
    }

    @Test
    fun testErrorOrder() {
        val result = ValidationResult.invalid(listOf(
            ValidationError("$.a", "First"),
            ValidationError("$.b", "Second"),
            ValidationError("$.c", "Third")
        ))
        val errors = result.errors
        assertEquals("$.a", errors[0].path)
        assertEquals("$.b", errors[1].path)
        assertEquals("$.c", errors[2].path)
    }

    @Test
    fun testErrorImmutability() {
        val mutableList = mutableListOf(ValidationError("$.field", "Error"))
        val result = ValidationResult.invalid(mutableList)
        mutableList.add(ValidationError("$.other", "Another"))
        assertEquals(1, result.errors.size)
    }

    @Test
    fun testNestedStructureErrors() {
        val schema = Schemas.`object`()
            .required("user", Schemas.`object`()
                .required("profile", Schemas.`object`()
                    .required("age", Schemas.number().withMinimum(0.0))
                    .required("email", Schemas.string())))

        val data = mapOf(
            "user" to mapOf(
                "profile" to mapOf(
                    "age" to -5.0,
                    "email" to 123
                )
            )
        )

        val result = schema.validate(data)
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.path.contains("age") })
        assertTrue(result.errors.any { it.path.contains("email") })
    }

    @Test
    fun testErrorMessages() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("age", Schemas.number())

        val result = schema.validate(mapOf("age" to 30))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("Required") })
    }

    @Test
    fun testTypeMismatchErrors() {
        val schema = Schemas.`object`()
            .required("count", Schemas.number())
            .required("active", Schemas.bool())

        val result = schema.validate(mapOf(
            "count" to "not a number",
            "active" to "not a boolean"
        ))
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.message.contains("number") })
        assertTrue(result.errors.any { it.message.contains("boolean") })
    }

    @Test
    fun testArrayErrorIndices() {
        val schema = Schemas.array(Schemas.number().withMinimum(0.0))

        val result = schema.validate(listOf(10, -5, 20, -10))
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.path.contains("[1]") })
        assertTrue(result.errors.any { it.path.contains("[3]") })
    }

    @Test
    fun testArrayConstraintErrors() {
        val schema = Schemas.array(Schemas.string())
            .withMinItems(2)
            .withMaxItems(4)

        val tooFew = schema.validate(listOf("a"))
        assertFalse(tooFew.isValid)
        assertTrue(tooFew.errors[0].message.contains("too few"))

        val tooMany = schema.validate(listOf("a", "b", "c", "d", "e"))
        assertFalse(tooMany.isValid)
        assertTrue(tooMany.errors[0].message.contains("too many"))
    }

    @Test
    fun testStringConstraintErrors() {
        val schema = Schemas.string()
            .withMinLength(5)
            .withMaxLength(10)

        val tooShort = schema.validate("hi")
        assertFalse(tooShort.isValid)
        assertTrue(tooShort.errors[0].message.contains("too short"))

        val tooLong = schema.validate("thisisareallylongstring")
        assertFalse(tooLong.isValid)
        assertTrue(tooLong.errors[0].message.contains("too long"))
    }

    @Test
    fun testNumberConstraintErrors() {
        val schema = Schemas.number()
            .withMinimum(0.0)
            .withMaximum(100.0)

        val tooSmall = schema.validate(-10)
        assertFalse(tooSmall.isValid)
        assertTrue(tooSmall.errors[0].message.contains("less than minimum"))

        val tooLarge = schema.validate(150)
        assertFalse(tooLarge.isValid)
        assertTrue(tooLarge.errors[0].message.contains("greater than maximum"))
    }

    @Test
    fun testErrorPathFormat() {
        val schema = Schemas.`object`()
            .required("items", Schemas.array(Schemas.`object`()
                .required("id", Schemas.number())))

        val data = mapOf(
            "items" to listOf(
                mapOf("id" to 1),
                mapOf("id" to "invalid")
            )
        )

        val result = schema.validate(data)
        assertFalse(result.isValid)
        val errorPath = result.errors[0].path
        assertTrue(errorPath.contains("$.items"))
        assertTrue(errorPath.contains("[1]"))
        assertTrue(errorPath.contains("id"))
    }
}
