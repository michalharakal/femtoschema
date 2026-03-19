package me.bechberger.util.femtoschema

internal object JsonSchemaParserHelpers {

    fun rejectUnknownKeywords(schema: Map<String, Any?>, allowed: Set<String>) {
        for (key in schema.keys) {
            if (key == "\$comment") continue
            if (key !in allowed) {
                throw IllegalArgumentException("Unsupported JSON Schema keyword: '$key'")
            }
        }
    }

    fun asInteger(obj: Any?): Int? {
        if (obj == null) return null
        return when (obj) {
            is Int -> obj
            is Long -> obj.toInt()
            is Number -> obj.toInt()
            else -> throw IllegalArgumentException("Expected integer, got ${typeOf(obj)}")
        }
    }

    fun asDouble(obj: Any?): Double? {
        if (obj == null) return null
        return when (obj) {
            is Number -> obj.toDouble()
            else -> throw IllegalArgumentException("Expected number, got ${typeOf(obj)}")
        }
    }

    fun asBoolean(obj: Any?): Boolean? {
        if (obj == null) return null
        return when (obj) {
            is Boolean -> obj
            else -> throw IllegalArgumentException("Expected boolean, got ${typeOf(obj)}")
        }
    }

    fun typeOf(obj: Any?): String {
        if (obj == null) return "null"
        return obj::class.simpleName?.lowercase() ?: "unknown"
    }
}

internal fun typeOf(obj: Any?): String = JsonSchemaParserHelpers.typeOf(obj)
