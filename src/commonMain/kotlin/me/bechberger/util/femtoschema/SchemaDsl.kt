package me.bechberger.util.femtoschema

@DslMarker
annotation class SchemaDslMarker

@SchemaDslMarker
class StringSchemaBuilder {
    var minLength: Int? = null
    var maxLength: Int? = null
    var pattern: String? = null
    var description: String = "string"

    fun build(): StringSchema = StringSchema(
        description = description,
        minLength = minLength,
        maxLength = maxLength,
        pattern = pattern
    )
}

@SchemaDslMarker
class NumberSchemaBuilder {
    var minimum: Double? = null
    var maximum: Double? = null
    var exclusiveMinimum: Double? = null
    var exclusiveMaximum: Double? = null
    var description: String = "number"

    fun build(): NumberSchema = NumberSchema(
        description = description,
        minimum = minimum,
        maximum = maximum,
        exclusiveMinimum = exclusiveMinimum,
        exclusiveMaximum = exclusiveMaximum
    )
}

@SchemaDslMarker
class BooleanSchemaBuilder {
    var description: String = "boolean"

    fun build(): BooleanSchema = BooleanSchema(description = description)
}

@SchemaDslMarker
class ArraySchemaBuilder(private val itemSchema: TypeSchema) {
    var minItems: Int? = null
    var maxItems: Int? = null
    var uniqueItems: Boolean = false
    var description: String = "array"

    fun build(): ArraySchema = ArraySchema(
        itemSchema = itemSchema,
        minItems = minItems,
        maxItems = maxItems,
        uniqueItems = uniqueItems,
        description = description
    )
}

@SchemaDslMarker
class ObjectSchemaBuilder {
    private val properties = linkedMapOf<String, TypeSchema>()
    private val requiredProperties = mutableSetOf<String>()
    var additionalProperties: Boolean = true
    var description: String = "object"

    fun required(name: String, schema: TypeSchema) {
        properties[name] = schema
        requiredProperties.add(name)
    }

    fun optional(name: String, schema: TypeSchema) {
        properties[name] = schema
    }

    fun build(): ObjectSchema = ObjectSchema(
        properties = LinkedHashMap(properties),
        requiredProperties = requiredProperties.toSet(),
        additionalPropertiesAllowed = additionalProperties,
        description = description
    )
}

@SchemaDslMarker
class SumTypeSchemaBuilder(private val discriminatorField: String) {
    private val variants = linkedMapOf<String, TypeSchema>()
    var description: String = "sum type"

    fun variant(discriminatorValue: String, schema: TypeSchema) {
        variants[discriminatorValue] = schema
    }

    fun build(): SumTypeSchema = SumTypeSchema(
        discriminatorField = discriminatorField,
        variants = LinkedHashMap(variants),
        description = description
    )
}

fun stringSchema(): StringSchema = StringSchema()

fun stringSchema(block: StringSchemaBuilder.() -> Unit): StringSchema =
    StringSchemaBuilder().apply(block).build()

fun numberSchema(): NumberSchema = NumberSchema()

fun numberSchema(block: NumberSchemaBuilder.() -> Unit): NumberSchema =
    NumberSchemaBuilder().apply(block).build()

fun booleanSchema(): BooleanSchema = BooleanSchema()

fun booleanSchema(block: BooleanSchemaBuilder.() -> Unit): BooleanSchema =
    BooleanSchemaBuilder().apply(block).build()

fun enumSchema(vararg values: Any?): EnumSchema = EnumSchema(*values)

fun arraySchema(itemSchema: TypeSchema): ArraySchema = ArraySchema(itemSchema)

fun arraySchema(itemSchema: TypeSchema, block: ArraySchemaBuilder.() -> Unit): ArraySchema =
    ArraySchemaBuilder(itemSchema).apply(block).build()

fun objectSchema(block: ObjectSchemaBuilder.() -> Unit): ObjectSchema =
    ObjectSchemaBuilder().apply(block).build()

fun sumTypeSchema(discriminatorField: String, block: SumTypeSchemaBuilder.() -> Unit): SumTypeSchema =
    SumTypeSchemaBuilder(discriminatorField).apply(block).build()
