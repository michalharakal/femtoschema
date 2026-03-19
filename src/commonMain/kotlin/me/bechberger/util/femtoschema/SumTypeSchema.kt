package me.bechberger.util.femtoschema

data class SumTypeSchema(
    val discriminatorField: String,
    val variants: Map<String, TypeSchema> = linkedMapOf(),
    override val description: String = "sum type"
) : TypeSchema {

    fun variant(discriminatorValue: String, schema: TypeSchema): SumTypeSchema {
        val newVariants = LinkedHashMap(variants)
        newVariants[discriminatorValue] = schema
        return copy(variants = newVariants)
    }

    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !is Map<*, *>) {
            return ValidationResult.invalid(path, "Expected object, got ${typeOf(value)}")
        }
        @Suppress("UNCHECKED_CAST")
        val map = value as Map<String, Any?>
        val discriminatorValue = map[discriminatorField]
            ?: return ValidationResult.invalid(
                "$path.$discriminatorField",
                "Discriminator field '$discriminatorField' is required"
            )
        val discriminatorStr = discriminatorValue.toString()
        val variantSchema = variants[discriminatorStr]
            ?: return ValidationResult.invalid(
                "$path.$discriminatorField",
                "Unknown discriminator value '$discriminatorStr'. Must be one of: ${variants.keys}"
            )
        return variantSchema.validate(value, path)
    }

    override fun toJsonSchema(): Map<String, Any?> = buildMap {
        val oneOfSchemas = variants.values.map { LinkedHashMap(it.toJsonSchema()) }
        put("oneOf", oneOfSchemas)
        put("discriminator", linkedMapOf(
            "propertyName" to discriminatorField,
            "mapping" to linkedMapOf<String, String>().apply {
                for (key in variants.keys) put(key, key)
            }
        ))
    }

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): SumTypeSchema {
            val discObj = schema["discriminator"]
            if (discObj !is Map<*, *>) {
                throw IllegalArgumentException("Only discriminated unions are supported for 'oneOf' (missing 'discriminator')")
            }
            @Suppress("UNCHECKED_CAST")
            val discMap = discObj as Map<String, Any?>
            val propertyName = discMap["propertyName"]
            if (propertyName !is String) {
                throw IllegalArgumentException("'discriminator.propertyName' must be a string")
            }
            val oneOfObj = schema["oneOf"]
            if (oneOfObj !is List<*>) {
                throw IllegalArgumentException("'oneOf' must be a list, got ${JsonSchemaParserHelpers.typeOf(oneOfObj)}")
            }
            var sum = Schemas.sumType(propertyName)
            for (variantObj in oneOfObj) {
                if (variantObj !is Map<*, *>) {
                    throw IllegalArgumentException("'oneOf' entries must be objects, got ${JsonSchemaParserHelpers.typeOf(variantObj)}")
                }
                @Suppress("UNCHECKED_CAST")
                val variantSchema = JsonSchemaParser.parse(variantObj as Map<String, Any?>)
                val discriminatorValue = inferDiscriminatorValue(propertyName, variantSchema)
                sum = sum.variant(discriminatorValue, variantSchema)
            }
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("oneOf", "discriminator"))
            return sum
        }

        private fun inferDiscriminatorValue(discriminatorField: String, variantSchema: TypeSchema): String {
            if (variantSchema !is ObjectSchema) {
                throw IllegalArgumentException("Sum type variants must be object schemas to infer discriminator value")
            }
            val exported = variantSchema.toJsonSchema()
            val propsObj = exported["properties"]
            if (propsObj !is Map<*, *>) {
                throw IllegalArgumentException("Variant object schema must have 'properties' to infer discriminator value")
            }
            @Suppress("UNCHECKED_CAST")
            val props = propsObj as Map<String, Any?>
            val discSchemaObj = props[discriminatorField]
            if (discSchemaObj !is Map<*, *>) {
                throw IllegalArgumentException("Variant schema must define discriminator property '$discriminatorField'")
            }
            @Suppress("UNCHECKED_CAST")
            val discSchema = discSchemaObj as Map<String, Any?>
            val enumObj = discSchema["enum"]
            if (enumObj !is List<*> || enumObj.size != 1) {
                throw IllegalArgumentException("Discriminator property must be an enum with exactly one value")
            }
            return enumObj[0].toString()
        }
    }
}
