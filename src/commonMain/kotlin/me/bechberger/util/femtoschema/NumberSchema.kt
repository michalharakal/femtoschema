package me.bechberger.util.femtoschema

data class NumberSchema(
    override val description: String = "number",
    val minimum: Double? = null,
    val maximum: Double? = null,
    val exclusiveMinimum: Double? = null,
    val exclusiveMaximum: Double? = null
) : TypeSchema {

    fun withMinimum(min: Double) = copy(minimum = min, exclusiveMinimum = null)
    fun withMaximum(max: Double) = copy(maximum = max, exclusiveMaximum = null)
    fun withExclusiveMinimum(min: Double) = copy(minimum = null, exclusiveMinimum = min)
    fun withExclusiveMaximum(max: Double) = copy(maximum = null, exclusiveMaximum = max)
    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        val numValue: Double = when (value) {
            is Number -> value.toDouble()
            else -> return ValidationResult.invalid(path, "Expected number, got ${typeOf(value)}")
        }
        if (minimum != null && numValue < minimum) {
            return ValidationResult.invalid(path, "Number less than minimum ($minimum)")
        }
        if (exclusiveMinimum != null && numValue <= exclusiveMinimum) {
            return ValidationResult.invalid(path, "Number less than or equal to exclusive minimum ($exclusiveMinimum)")
        }
        if (maximum != null && numValue > maximum) {
            return ValidationResult.invalid(path, "Number greater than maximum ($maximum)")
        }
        if (exclusiveMaximum != null && numValue >= exclusiveMaximum) {
            return ValidationResult.invalid(path, "Number greater than or equal to exclusive maximum ($exclusiveMaximum)")
        }
        return ValidationResult.valid()
    }

    override fun toJsonSchema(): Map<String, Any?> = buildMap {
        put("type", "number")
        if (minimum != null) put("minimum", minimum)
        if (exclusiveMinimum != null) put("exclusiveMinimum", exclusiveMinimum)
        if (maximum != null) put("maximum", maximum)
        if (exclusiveMaximum != null) put("exclusiveMaximum", exclusiveMaximum)
    }

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): NumberSchema {
            var s = Schemas.number()
            val min = JsonSchemaParserHelpers.asDouble(schema["minimum"])
            val max = JsonSchemaParserHelpers.asDouble(schema["maximum"])
            val exMin = JsonSchemaParserHelpers.asDouble(schema["exclusiveMinimum"])
            val exMax = JsonSchemaParserHelpers.asDouble(schema["exclusiveMaximum"])
            if (min != null) s = s.withMinimum(min)
            if (max != null) s = s.withMaximum(max)
            if (exMin != null) s = s.withExclusiveMinimum(exMin)
            if (exMax != null) s = s.withExclusiveMaximum(exMax)
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("type", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum"))
            return s
        }
    }
}
