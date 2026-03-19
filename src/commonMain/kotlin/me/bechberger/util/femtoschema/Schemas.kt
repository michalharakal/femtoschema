package me.bechberger.util.femtoschema

import me.bechberger.util.femtoschema.json.JsonParser
import me.bechberger.util.femtoschema.json.JsonPrinter

object Schemas {

    fun string() = StringSchema()
    fun number() = NumberSchema()
    fun bool() = BooleanSchema()
    fun `object`() = ObjectSchema()
    fun array(itemSchema: TypeSchema) = ArraySchema(itemSchema)

    fun enumOf(vararg values: Any?): EnumSchema = EnumSchema(values.toSet())
    fun enumOf(values: Set<Any?>): EnumSchema = EnumSchema(values)

    fun sumType(discriminatorField: String) = SumTypeSchema(discriminatorField)

    fun toJsonSchemaString(schema: TypeSchema): String =
        JsonPrinter.prettyPrint(schema.toJsonSchema())

    fun fromJsonSchema(jsonSchema: Any?): TypeSchema =
        JsonSchemaParser.parse(jsonSchema)

    fun fromJsonSchemaString(jsonSchema: String): TypeSchema {
        try {
            return fromJsonSchema(JsonParser.parse(jsonSchema))
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON schema string: ${e.message}", e)
        }
    }
}
