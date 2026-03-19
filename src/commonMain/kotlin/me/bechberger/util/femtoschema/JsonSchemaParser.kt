package me.bechberger.util.femtoschema

internal object JsonSchemaParser {

    fun parse(jsonSchema: Any?): TypeSchema {
        if (jsonSchema !is Map<*, *>) {
            throw IllegalArgumentException("JSON schema must be an object/map, got ${JsonSchemaParserHelpers.typeOf(jsonSchema)}")
        }
        @Suppress("UNCHECKED_CAST")
        return parseSchemaObject(jsonSchema as Map<String, Any?>)
    }

    private fun parseSchemaObject(schema: Map<String, Any?>): TypeSchema {
        if ("oneOf" in schema) return SumTypeSchema.fromJsonSchema(schema)
        if ("enum" in schema) return EnumSchema.fromJsonSchema(schema)

        val type = schema["type"]
        if (type !is String) {
            throw IllegalArgumentException("Unsupported JSON schema object (missing 'type', 'enum', or 'oneOf'): ${schema.keys}")
        }
        return when (type) {
            "string" -> StringSchema.fromJsonSchema(schema)
            "number" -> NumberSchema.fromJsonSchema(schema)
            "boolean" -> BooleanSchema.fromJsonSchema(schema)
            "array" -> ArraySchema.fromJsonSchema(schema)
            "object" -> ObjectSchema.fromJsonSchema(schema)
            else -> throw IllegalArgumentException("Unsupported JSON schema type: $type")
        }
    }
}
