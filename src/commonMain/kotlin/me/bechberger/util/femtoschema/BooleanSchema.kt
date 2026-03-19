package me.bechberger.util.femtoschema

data class BooleanSchema(
    override val description: String = "boolean"
) : TypeSchema {

    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !is Boolean) {
            return ValidationResult.invalid(path, "Expected boolean, got ${typeOf(value)}")
        }
        return ValidationResult.valid()
    }

    override fun toJsonSchema(): Map<String, Any?> = mapOf("type" to "boolean")

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): BooleanSchema {
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("type"))
            return Schemas.bool()
        }
    }
}
