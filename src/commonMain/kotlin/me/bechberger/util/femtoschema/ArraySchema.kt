package me.bechberger.util.femtoschema

data class ArraySchema(
    val itemSchema: TypeSchema,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean = false,
    override val description: String = "array"
) : TypeSchema {

    fun withMinItems(min: Int) = copy(minItems = min)
    fun withMaxItems(max: Int) = copy(maxItems = max)
    fun withUniqueItems(unique: Boolean) = copy(uniqueItems = unique)
    fun withDescription(desc: String) = copy(description = desc)

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !is List<*>) {
            return ValidationResult.invalid(path, "Expected array, got ${typeOf(value)}")
        }
        val errors = mutableListOf<ValidationError>()
        if (minItems != null && value.size < minItems) {
            errors.add(ValidationError(path, "Array has too few items (minimum $minItems)"))
        }
        if (maxItems != null && value.size > maxItems) {
            errors.add(ValidationError(path, "Array has too many items (maximum $maxItems)"))
        }
        val seenItems = if (uniqueItems) mutableSetOf<Any?>() else null
        for (i in value.indices) {
            val item = value[i]
            val itemPath = "$path[$i]"
            val result = itemSchema.validate(item, itemPath)
            if (!result.isValid) {
                errors.addAll(result.errors)
            }
            if (uniqueItems && !seenItems!!.add(item)) {
                errors.add(ValidationError(itemPath, "Array items must be unique"))
            }
        }
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }

    override fun toJsonSchema(): Map<String, Any?> = buildMap {
        put("type", "array")
        put("items", itemSchema.toJsonSchema())
        if (minItems != null) put("minItems", minItems)
        if (maxItems != null) put("maxItems", maxItems)
        if (uniqueItems) put("uniqueItems", true)
    }

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): ArraySchema {
            val itemsObj = schema["items"]
            if (itemsObj !is Map<*, *>) {
                throw IllegalArgumentException("Array schema must have an 'items' schema object")
            }
            @Suppress("UNCHECKED_CAST")
            val itemSchema = JsonSchemaParser.parse(itemsObj as Map<String, Any?>)
            var s = Schemas.array(itemSchema)
            val min = JsonSchemaParserHelpers.asInteger(schema["minItems"])
            val max = JsonSchemaParserHelpers.asInteger(schema["maxItems"])
            val unique = JsonSchemaParserHelpers.asBoolean(schema["uniqueItems"])
            if (min != null) s = s.withMinItems(min)
            if (max != null) s = s.withMaxItems(max)
            if (unique != null) s = s.withUniqueItems(unique)
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("type", "items", "minItems", "maxItems", "uniqueItems"))
            return s
        }
    }
}
