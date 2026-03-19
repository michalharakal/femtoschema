package me.bechberger.util.femtoschema

import kotlin.test.*

class JsonSchemaParseTest {

    @Test
    fun testRoundTripComplexSchema() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string().withMinLength(1).withMaxLength(100))
            .required("age", Schemas.number().withMinimum(0.0).withMaximum(150.0))
            .required("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("tags", Schemas.array(Schemas.string()).withMinItems(0).withMaxItems(10))
            .allowAdditionalProperties(false)

        val exported = schema.toJsonSchema()
        val parsed = Schemas.fromJsonSchema(exported)
        assertEquals(exported, parsed.toJsonSchema())
    }

    @Test
    fun testIgnoreComment() {
        val schema = Schemas.array(
            Schemas.`object`()
                .required("status", Schemas.enumOf("PENDING", "ACTIVE"))
                .optional("flag", Schemas.bool())
        ).withMinItems(1)

        val exported = LinkedHashMap(schema.toJsonSchema())
        exported["\$comment"] = "top-level comment"

        @Suppress("UNCHECKED_CAST")
        val items = LinkedHashMap(exported["items"] as Map<String, Any?>)
        items["\$comment"] = "items comment"
        exported["items"] = items

        @Suppress("UNCHECKED_CAST")
        val props = LinkedHashMap(items["properties"] as Map<String, Any?>)
        @Suppress("UNCHECKED_CAST")
        val status = LinkedHashMap(props["status"] as Map<String, Any?>)
        status["\$comment"] = "enum comment"
        props["status"] = status
        items["properties"] = props
        exported["items"] = items

        val parsed = Schemas.fromJsonSchema(exported)
        assertEquals(schema.toJsonSchema(), parsed.toJsonSchema())
    }

    @Test
    fun testFromJsonSchemaString() {
        val json = """{"type":"array","${"$"}comment":"top","minItems":1,"items":{"type":"string","${"$"}comment":"nested","minLength":2}}"""

        val parsed = Schemas.fromJsonSchemaString(json)
        val expected = Schemas.array(Schemas.string().withMinLength(2)).withMinItems(1)
        assertEquals(expected.toJsonSchema(), parsed.toJsonSchema())
    }
}
