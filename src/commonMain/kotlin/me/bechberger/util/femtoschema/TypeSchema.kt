package me.bechberger.util.femtoschema

sealed interface TypeSchema {
    fun validate(value: Any?): ValidationResult
    fun validate(value: Any?, path: String): ValidationResult
    fun toJsonSchema(): Map<String, Any?>
    val description: String
}
