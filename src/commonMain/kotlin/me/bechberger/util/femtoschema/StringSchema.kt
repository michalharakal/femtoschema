package me.bechberger.util.femtoschema

data class StringSchema(
    override val description: String = "string",
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null
) : TypeSchema {

    fun withMinLength(length: Int) = copy(minLength = length)
    fun withMaxLength(length: Int) = copy(maxLength = length)
    fun withPattern(regex: String) = copy(pattern = regex)
    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !is String) {
            return ValidationResult.invalid(path, "Expected string, got ${typeOf(value)}")
        }
        if (minLength != null && value.length < minLength) {
            return ValidationResult.invalid(path, "String too short (minimum $minLength characters)")
        }
        if (maxLength != null && value.length > maxLength) {
            return ValidationResult.invalid(path, "String too long (maximum $maxLength characters)")
        }
        if (pattern != null && !Regex(pattern).matches(value)) {
            return ValidationResult.invalid(path, "String does not match pattern: $pattern")
        }
        return ValidationResult.valid()
    }

    override fun toJsonSchema(): Map<String, Any?> = buildMap {
        put("type", "string")
        if (minLength != null) put("minLength", minLength)
        if (maxLength != null) put("maxLength", maxLength)
        if (pattern != null) put("pattern", pattern)
    }

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): StringSchema {
            var s = Schemas.string()
            val min = JsonSchemaParserHelpers.asInteger(schema["minLength"])
            val max = JsonSchemaParserHelpers.asInteger(schema["maxLength"])
            val pat = schema["pattern"]
            if (min != null) s = s.withMinLength(min)
            if (max != null) s = s.withMaxLength(max)
            if (pat is String) s = s.withPattern(pat)
            else if (pat != null) throw IllegalArgumentException("'pattern' must be a string")
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("type", "minLength", "maxLength", "pattern"))
            return s
        }
    }
}
