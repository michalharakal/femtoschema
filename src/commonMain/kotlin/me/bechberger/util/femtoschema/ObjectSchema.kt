package me.bechberger.util.femtoschema

data class ObjectSchema(
    val properties: Map<String, TypeSchema> = linkedMapOf(),
    val requiredProperties: Set<String> = emptySet(),
    val additionalPropertiesAllowed: Boolean = true,
    override val description: String = "object"
) : TypeSchema {

    fun property(name: String, schema: TypeSchema) = required(name, schema)

    fun required(name: String, schema: TypeSchema): ObjectSchema {
        val newProps = LinkedHashMap(properties)
        newProps[name] = schema
        return copy(properties = newProps, requiredProperties = requiredProperties + name)
    }

    fun optional(name: String, schema: TypeSchema): ObjectSchema {
        val newProps = LinkedHashMap(properties)
        newProps[name] = schema
        return copy(properties = newProps)
    }

    fun allowAdditionalProperties(allow: Boolean) = copy(additionalPropertiesAllowed = allow)
    fun withDescription(desc: String) = copy(description = desc)

    fun requiredKeys(): Set<String> = requiredProperties.toSet()

    override fun validate(value: Any?): ValidationResult = validate(value, "$")

    override fun validate(value: Any?, path: String): ValidationResult {
        if (value !is Map<*, *>) {
            return ValidationResult.invalid(path, "Expected object, got ${typeOf(value)}")
        }
        @Suppress("UNCHECKED_CAST")
        val map = value as Map<String, Any?>
        val errors = mutableListOf<ValidationError>()

        for (propName in requiredProperties) {
            if (!map.containsKey(propName)) {
                errors.add(ValidationError("$path.$propName", "Required property missing"))
            }
        }
        for ((propName, propValue) in map) {
            val propPath = "$path.$propName"
            if (propName in properties) {
                val result = properties[propName]!!.validate(propValue, propPath)
                if (!result.isValid) {
                    errors.addAll(result.errors)
                }
            } else if (!additionalPropertiesAllowed) {
                errors.add(ValidationError(propPath, "Additional properties are not allowed"))
            }
        }
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }

    override fun toJsonSchema(): Map<String, Any?> = buildMap {
        put("type", "object")
        if (properties.isNotEmpty()) {
            put("properties", properties.mapValues { (_, v) -> v.toJsonSchema() })
        }
        if (requiredProperties.isNotEmpty()) {
            put("required", requiredProperties.toList())
        }
        put("additionalProperties", additionalPropertiesAllowed)
    }

    internal companion object {
        fun fromJsonSchema(schema: Map<String, Any?>): ObjectSchema {
            var s = Schemas.`object`()
            val propsObj = schema["properties"]
            var props: Map<String, Any?>? = null
            if (propsObj != null) {
                if (propsObj !is Map<*, *>) {
                    throw IllegalArgumentException("'properties' must be an object/map")
                }
                @Suppress("UNCHECKED_CAST")
                props = propsObj as Map<String, Any?>
            }
            val required = mutableSetOf<String>()
            val requiredObj = schema["required"]
            if (requiredObj != null) {
                if (requiredObj !is List<*>) {
                    throw IllegalArgumentException("'required' must be a list")
                }
                for (o in requiredObj) {
                    if (o !is String) throw IllegalArgumentException("'required' entries must be strings")
                    required.add(o)
                }
            }
            if (required.isNotEmpty() && props == null) {
                throw IllegalArgumentException("'required' is present but 'properties' is missing")
            }
            if (props != null) {
                for ((name, propSchemaObj) in props) {
                    if (propSchemaObj !is Map<*, *>) {
                        throw IllegalArgumentException("Property schema for '$name' must be an object")
                    }
                    @Suppress("UNCHECKED_CAST")
                    val parsed = JsonSchemaParser.parse(propSchemaObj as Map<String, Any?>)
                    s = if (name in required) s.required(name, parsed) else s.optional(name, parsed)
                }
            }
            val additionalProps = JsonSchemaParserHelpers.asBoolean(schema["additionalProperties"])
            if (additionalProps != null) {
                s = s.allowAdditionalProperties(additionalProps)
            }
            JsonSchemaParserHelpers.rejectUnknownKeywords(schema, setOf("type", "properties", "required", "additionalProperties"))
            return s
        }
    }
}
