package me.bechberger.util.femtoschema

sealed interface ValidationResult {
    val isValid: Boolean
    val errors: List<ValidationError>

    data object Valid : ValidationResult {
        override val isValid: Boolean get() = true
        override val errors: List<ValidationError> get() = emptyList()
    }

    data class Invalid(override val errors: List<ValidationError>) : ValidationResult {
        constructor(error: ValidationError) : this(listOf(error))

        override val isValid: Boolean get() = false
    }

    companion object {
        fun valid(): Valid = Valid

        fun invalid(path: String, message: String): Invalid =
            Invalid(ValidationError(path, message))

        fun invalid(errors: List<ValidationError>): Invalid =
            Invalid(errors.toList())
    }
}

data class ValidationError(
    val path: String,
    val message: String
)
