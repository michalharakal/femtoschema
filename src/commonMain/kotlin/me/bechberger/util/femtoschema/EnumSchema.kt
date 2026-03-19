package me.bechberger.util.femtoschema

data class EnumSchema(
    val allowedValues: Set<Any?>,
    override val description: String = "enum"
) : TypeSchema {

    constructor(vararg values: Any?) : this(values.toSet())

    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !in allowedValues) {
            return ValidationResult.invalid(path, "Value must be one of: $allowedValues")
        }
        return ValidationResult.valid()
    }

    override fun toJsonSchema(): Map<String, Any?> = mapOf("enum" to allowedValues.toList())

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): EnumSchema {
            val enumObj = schema["enum"]
            if (enumObj !is List<*>) {
                throw IllegalArgumentException("'enum' must be a list, got ${JsonSchemaParserHelpers.typeOf(enumObj)}")
            }
            val values = LinkedHashSet<Any?>(enumObj)
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("enum"))
            return Schemas.enumOf(values)
        }
    }
}
