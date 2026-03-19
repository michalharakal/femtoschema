package me.bechberger.util.femtoschema

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.*

class JsonSchemaOracleTest {

    private val mapper = ObjectMapper()
    private val schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    @Test
    fun testComplexObjectSchema() {
        val schema = Schemas.`object`()
            .required("name", Schemas.string().withMinLength(1).withMaxLength(100))
            .required("age", Schemas.number().withMinimum(0.0).withMaximum(150.0))
            .required("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("tags", Schemas.array(Schemas.string()).withMinItems(0).withMaxItems(10))
            .allowAdditionalProperties(false)

        val schemaMap = schema.toJsonSchema()
        val schemaJson = mapper.writeValueAsString(schemaMap)
        val jsonSchema = schemaRegistry.getSchema(schemaJson)

        // Valid object
        val validData = linkedMapOf<String, Any?>(
            "name" to "Alice",
            "age" to 30,
            "email" to "alice@example.com",
            "tags" to listOf("engineer", "java")
        )
        val validJson = mapper.writeValueAsString(validData)
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty())

        // Missing required field
        val missingRequired = linkedMapOf<String, Any?>("name" to "Bob", "age" to 25)
        val missingJson = mapper.writeValueAsString(missingRequired)
        assertFalse(jsonSchema.validate(missingJson, InputFormat.JSON).isEmpty())

        // Invalid email format
        val invalidEmail = LinkedHashMap(validData)
        invalidEmail["email"] = "not-an-email"
        val invalidJson = mapper.writeValueAsString(invalidEmail)
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty())

        // Age out of range
        val invalidAge = LinkedHashMap(validData)
        invalidAge["age"] = 200
        val ageJson = mapper.writeValueAsString(invalidAge)
        assertFalse(jsonSchema.validate(ageJson, InputFormat.JSON).isEmpty())
    }

    @Test
    fun testDeeplyNestedSchema() {
        val addressSchema = Schemas.`object`()
            .required("street", Schemas.string())
            .required("city", Schemas.string())
            .required("zipCode", Schemas.string().withPattern("^\\d{5}$"))

        val companySchema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("address", addressSchema)

        val personSchema = Schemas.`object`()
            .required("name", Schemas.string())
            .required("company", companySchema)
            .required("yearsExperience", Schemas.number().withMinimum(0.0))

        val schemaMap = personSchema.toJsonSchema()
        val schemaJson = mapper.writeValueAsString(schemaMap)
        val jsonSchema = schemaRegistry.getSchema(schemaJson)

        val address = linkedMapOf<String, Any?>("street" to "123 Main St", "city" to "Springfield", "zipCode" to "12345")
        val company = linkedMapOf<String, Any?>("name" to "TechCorp", "address" to address)
        val person = linkedMapOf<String, Any?>("name" to "Alice", "company" to company, "yearsExperience" to 10)

        val validJson = mapper.writeValueAsString(person)
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty())

        // Invalid zip code
        val invalidAddress = LinkedHashMap(address)
        invalidAddress["zipCode"] = "invalid"
        val invalidCompany = LinkedHashMap(company)
        invalidCompany["address"] = invalidAddress
        val invalidPerson = LinkedHashMap(person)
        invalidPerson["company"] = invalidCompany

        val invalidJson = mapper.writeValueAsString(invalidPerson)
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty())
    }

    @Test
    fun testArrayOfConstrainedObjects() {
        val itemSchema = Schemas.`object`()
            .required("id", Schemas.number().withMinimum(1.0))
            .required("name", Schemas.string().withMinLength(1).withMaxLength(50))
            .required("quantity", Schemas.number().withMinimum(0.0))
            .required("status", Schemas.enumOf("PENDING", "ACTIVE", "COMPLETED"))

        val schema = Schemas.array(itemSchema)
            .withMinItems(1)
            .withMaxItems(100)

        val schemaMap = schema.toJsonSchema()
        val schemaJson = mapper.writeValueAsString(schemaMap)
        val jsonSchema = schemaRegistry.getSchema(schemaJson)

        val item1 = linkedMapOf<String, Any?>("id" to 1, "name" to "Item A", "quantity" to 10, "status" to "ACTIVE")
        val item2 = linkedMapOf<String, Any?>("id" to 2, "name" to "Item B", "quantity" to 5, "status" to "PENDING")

        val validJson = mapper.writeValueAsString(listOf(item1, item2))
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty())

        // Too few items
        val emptyJson = mapper.writeValueAsString(emptyList<Any>())
        assertFalse(jsonSchema.validate(emptyJson, InputFormat.JSON).isEmpty())

        // Invalid enum value
        val invalidItem = LinkedHashMap(item1)
        invalidItem["status"] = "INVALID"
        val invalidJson = mapper.writeValueAsString(listOf(invalidItem))
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty())
    }

    @Test
    fun testComplexSchemaWithOptionals() {
        val contactSchema = Schemas.`object`()
            .optional("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("phone", Schemas.string().withPattern("^\\d{10}$"))
            .allowAdditionalProperties(false)

        val schema = Schemas.`object`()
            .required("id", Schemas.number().withMinimum(1.0))
            .required("name", Schemas.string())
            .optional("contact", contactSchema)
            .optional("preferences", Schemas.`object`()
                .optional("notifications", Schemas.bool())
                .optional("newsletter", Schemas.bool()))
            .allowAdditionalProperties(false)

        val schemaMap = schema.toJsonSchema()
        val schemaJson = mapper.writeValueAsString(schemaMap)
        val jsonSchema = schemaRegistry.getSchema(schemaJson)

        // Valid with all optional fields
        val fullData = linkedMapOf<String, Any?>(
            "id" to 1,
            "name" to "Test User",
            "contact" to linkedMapOf<String, Any?>("email" to "user@example.com", "phone" to "1234567890"),
            "preferences" to linkedMapOf<String, Any?>("notifications" to true, "newsletter" to false)
        )
        val fullJson = mapper.writeValueAsString(fullData)
        assertTrue(jsonSchema.validate(fullJson, InputFormat.JSON).isEmpty())

        // Valid with minimal required fields
        val minimalData = linkedMapOf<String, Any?>("id" to 2, "name" to "Minimal User")
        val minimalJson = mapper.writeValueAsString(minimalData)
        assertTrue(jsonSchema.validate(minimalJson, InputFormat.JSON).isEmpty())

        // Invalid phone format
        val badContact = linkedMapOf<String, Any?>("email" to "user@example.com", "phone" to "123")
        val invalidContactData = LinkedHashMap(fullData)
        invalidContactData["contact"] = badContact
        val invalidJson = mapper.writeValueAsString(invalidContactData)
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty())
    }
}
