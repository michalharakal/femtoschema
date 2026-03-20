package me.bechberger.util.femtoschema

import kotlin.test.*

class SchemaDslTest {

    @Test
    fun testStringSchemaDefaults() {
        assertEquals(Schemas.string(), stringSchema())
    }

    @Test
    fun testStringSchemaWithConstraints() {
        val dsl = stringSchema {
            minLength = 1
            maxLength = 100
            pattern = "[a-z]+"
            description = "a lowercase identifier"
        }
        val fluent = Schemas.string()
            .withMinLength(1)
            .withMaxLength(100)
            .withPattern("[a-z]+")
            .withDescription("a lowercase identifier")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testNumberSchemaDefaults() {
        assertEquals(Schemas.number(), numberSchema())
    }

    @Test
    fun testNumberSchemaWithConstraints() {
        val dsl = numberSchema {
            minimum = 0.0
            maximum = 100.0
            description = "a percentage"
        }
        val fluent = Schemas.number()
            .withMinimum(0.0)
            .withMaximum(100.0)
            .withDescription("a percentage")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testNumberSchemaExclusiveBounds() {
        val dsl = numberSchema {
            exclusiveMinimum = 0.0
            exclusiveMaximum = 1.0
        }
        val fluent = Schemas.number()
            .withExclusiveMinimum(0.0)
            .withExclusiveMaximum(1.0)
        assertEquals(fluent, dsl)
    }

    @Test
    fun testBooleanSchemaDefaults() {
        assertEquals(Schemas.bool(), booleanSchema())
    }

    @Test
    fun testBooleanSchemaWithDescription() {
        val dsl = booleanSchema { description = "is active" }
        val fluent = Schemas.bool().withDescription("is active")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testEnumSchema() {
        val dsl = enumSchema("red", "green", "blue")
        val fluent = Schemas.enumOf("red", "green", "blue")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testArraySchemaDefaults() {
        val dsl = arraySchema(stringSchema())
        val fluent = Schemas.array(Schemas.string())
        assertEquals(fluent, dsl)
    }

    @Test
    fun testArraySchemaWithConstraints() {
        val dsl = arraySchema(stringSchema()) {
            minItems = 1
            maxItems = 10
            uniqueItems = true
            description = "tags"
        }
        val fluent = Schemas.array(Schemas.string())
            .withMinItems(1)
            .withMaxItems(10)
            .withUniqueItems(true)
            .withDescription("tags")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testObjectSchema() {
        val dsl = objectSchema {
            required("name", stringSchema { minLength = 1 })
            required("age", numberSchema { minimum = 0.0 })
            optional("email", stringSchema())
            additionalProperties = false
            description = "a user"
        }
        val fluent = Schemas.`object`()
            .required("name", Schemas.string().withMinLength(1))
            .required("age", Schemas.number().withMinimum(0.0))
            .optional("email", Schemas.string())
            .allowAdditionalProperties(false)
            .withDescription("a user")
        assertEquals(fluent, dsl)
    }

    @Test
    fun testSumTypeSchema() {
        val dsl = sumTypeSchema("type") {
            variant("email", objectSchema {
                required("type", enumSchema("email"))
                required("address", stringSchema())
            })
            variant("sms", objectSchema {
                required("type", enumSchema("sms"))
                required("phoneNumber", stringSchema())
            })
        }
        val fluent = Schemas.sumType("type")
            .variant("email", Schemas.`object`()
                .required("type", Schemas.enumOf("email"))
                .required("address", Schemas.string())
            )
            .variant("sms", Schemas.`object`()
                .required("type", Schemas.enumOf("sms"))
                .required("phoneNumber", Schemas.string())
            )
        assertEquals(fluent, dsl)
    }

    @Test
    fun testNestedObjects() {
        val dsl = objectSchema {
            required("address", objectSchema {
                required("street", stringSchema())
                required("city", stringSchema())
                required("zip", stringSchema { pattern = "\\d{5}" })
            })
        }
        val fluent = Schemas.`object`()
            .required("address", Schemas.`object`()
                .required("street", Schemas.string())
                .required("city", Schemas.string())
                .required("zip", Schemas.string().withPattern("\\d{5}"))
            )
        assertEquals(fluent, dsl)
    }

    @Test
    fun testJsonSchemaOutputMatches() {
        val dsl = objectSchema {
            required("name", stringSchema { minLength = 1 })
            required("tags", arraySchema(stringSchema()) { uniqueItems = true })
        }
        val fluent = Schemas.`object`()
            .required("name", Schemas.string().withMinLength(1))
            .required("tags", Schemas.array(Schemas.string()).withUniqueItems(true))
        assertEquals(fluent.toJsonSchema(), dsl.toJsonSchema())
    }

    @Test
    fun testDslValidation() {
        val schema = objectSchema {
            required("name", stringSchema { minLength = 1 })
            required("age", numberSchema { minimum = 0.0 })
        }
        assertTrue(schema.validate(mapOf("name" to "Alice", "age" to 25.0)).isValid)
        assertFalse(schema.validate(mapOf("name" to "", "age" to 25.0)).isValid)
        assertFalse(schema.validate(mapOf("name" to "Alice", "age" to -1.0)).isValid)
        assertFalse(schema.validate(mapOf("age" to 25.0)).isValid)
    }

    @Test
    fun testDescriptionOnAllTypes() {
        assertEquals("s", stringSchema { description = "s" }.description)
        assertEquals("n", numberSchema { description = "n" }.description)
        assertEquals("b", booleanSchema { description = "b" }.description)
        assertEquals("a", arraySchema(stringSchema()) { description = "a" }.description)
        assertEquals("o", objectSchema { description = "o" }.description)
        assertEquals("st", sumTypeSchema("t") { description = "st" }.description)
    }
}
