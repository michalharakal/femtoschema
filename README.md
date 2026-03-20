# femtoschema

A tiny JSON Schema DSL for Kotlin Multiplatform that is built for simplicity
and not for performance, alongside [femtojson](https://github.com/parttimenerd/femtojson).
It provides a fluent API to define schemas,
validate JSON-like values (maps, lists, strings, numbers, booleans, null),
and export to a [JSON Schema (Draft 2020-12)](https://json-schema.org/draft/2020-12) representation.

It does not aim to support every feature of JSON Schema, see below for the supported features.

## Supported Platforms

| Platform       | Target         |
|----------------|----------------|
| JVM            | `jvm`          |
| JavaScript     | `js` (Node.js) |
| Linux x64      | `linuxX64`     |
| macOS x64      | `macosX64`     |
| macOS ARM64    | `macosArm64`   |
| Windows x64    | `mingwX64`     |

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("me.bechberger.util:femtoschema:0.2.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'me.bechberger.util:femtoschema:0.2.0'
}
```

### Maven (JVM only)

```xml
<dependency>
    <groupId>me.bechberger.util</groupId>
    <artifactId>femtoschema-jvm</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Usage

### Quick Start: define + validate

```kotlin
import me.bechberger.util.femtoschema.Schemas
import me.bechberger.util.femtoschema.ValidationResult

fun main() {
    val userSchema = Schemas.`object`()
        .required("name", Schemas.string().withMinLength(1))
        .required("age", Schemas.number().withMinimum(0))
        .optional("email", Schemas.string())

    val userData = mapOf(
        "name" to "Alice",
        "age" to 30.0,
        "email" to "alice@example.com"
    )

    val result = userSchema.validate(userData)

    if (!result.isValid()) {
        result.errors.forEach { e ->
            println("${e.path}: ${e.message}")
        }
    }
}
```

### Building object schemas

```kotlin
import me.bechberger.util.femtoschema.Schemas

val personSchema = Schemas.`object`()
    .required("name", Schemas.string())
    .required("age", Schemas.number().withMinimum(0).withMaximum(150))
    .optional("email", Schemas.string())
```

### Nested objects and arrays

```kotlin
import me.bechberger.util.femtoschema.Schemas

val addressSchema = Schemas.`object`()
    .required("street", Schemas.string())
    .required("city", Schemas.string())
    .required("zipCode", Schemas.string())

val employeeSchema = Schemas.`object`()
    .required("name", Schemas.string())
    .required("address", addressSchema)

val employeesSchema = Schemas.array(employeeSchema)

val employees = listOf(
    mapOf(
        "name" to "Alice",
        "address" to mapOf(
            "street" to "123 Main St",
            "city" to "Springfield",
            "zipCode" to "12345"
        )
    )
)

assert(employeesSchema.validate(employees).isValid())
```

### Enums

```kotlin
import me.bechberger.util.femtoschema.Schemas

val status = Schemas.enumOf("ACTIVE", "INACTIVE", "SUSPENDED")
assert(status.validate("ACTIVE").isValid())
assert(!status.validate("UNKNOWN").isValid())
```

### Sum types (discriminated unions)

```kotlin
import me.bechberger.util.femtoschema.Schemas

val notificationSchema = Schemas.sumType("type")
    .variant("email", Schemas.`object`()
        .required("type", Schemas.enumOf("email"))
        .required("address", Schemas.string()))
    .variant("sms", Schemas.`object`()
        .required("type", Schemas.enumOf("sms"))
        .required("phoneNumber", Schemas.string()))

assert(notificationSchema.validate(mapOf(
    "type" to "email",
    "address" to "user@example.com"
)).isValid())
```

## Validation

### Path-aware error reporting

Errors include the JSON path to the invalid value:

```kotlin
import me.bechberger.util.femtoschema.Schemas

val schema = Schemas.`object`()
    .required("user", Schemas.`object`()
        .required("profile", Schemas.`object`()
            .required("age", Schemas.number().withMinimum(0))))

val data = mapOf(
    "user" to mapOf(
        "profile" to mapOf(
            "age" to -5.0
        )
    )
)

val result = schema.validate(data)
result.errors.forEach { e ->
    println("${e.path}: ${e.message}")
}
// e.g. $.user.profile.age: Number less than minimum (0.0)
```

### Validating with a custom root path

```kotlin
import me.bechberger.util.femtoschema.Schemas

val schema = Schemas.string()
val result = schema.validate("hello", "$.config.name")
assert(result.isValid())
```

## JSON Schema export

Export your schema into a JSON-Schema-shaped `Map`:

```kotlin
import me.bechberger.util.femtoschema.Schemas

val schema = Schemas.`object`()
    .required("name", Schemas.string().withMinLength(1))
    .required("age", Schemas.number().withMinimum(0))
    .optional("email", Schemas.string())

val jsonSchema: Map<String, Any?> = schema.toJsonSchema()
```

Pretty print the exported schema (via `femtojson`):

```kotlin
import me.bechberger.util.femtoschema.Schemas

println(Schemas.toJsonSchemaString(schema))
```

## JSON Schema import (read schemas back)

If you already have a JSON Schema object (for example parsed as a `Map<String, Any?>`), you can convert it back into a `TypeSchema`:

```kotlin
import me.bechberger.util.femtoschema.Schemas
import me.bechberger.util.femtoschema.TypeSchema

val jsonSchema: Any? = /* decoded JSON schema object */
val schema = Schemas.fromJsonSchema(jsonSchema)
```

If you have the schema as a JSON string, you can use:

```kotlin
import me.bechberger.util.femtoschema.Schemas
import me.bechberger.util.femtoschema.TypeSchema

val json = """{"type":"string","minLength":1}"""
val schema = Schemas.fromJsonSchemaString(json)
```

Notes:

- The importer supports the same (small) subset of JSON Schema keywords that `toJsonSchema()` produces.
- The keyword `$comment` is ignored at any nesting level.

## Common constraints

### Strings

```kotlin
import me.bechberger.util.femtoschema.Schemas

val username = Schemas.string()
    .withMinLength(3)
    .withMaxLength(20)
    .withPattern("^[A-Za-z0-9_]+$")
```

### Numbers

`NumberSchema` supports inclusive and exclusive bounds. Note that *exclusive* bounds are values (Draft 2020-12 style), not boolean flags:

```kotlin
import me.bechberger.util.femtoschema.Schemas

val percentage = Schemas.number()
    .withExclusiveMinimum(0)
    .withExclusiveMaximum(100)
```

## Project Structure

```
src/
├── commonMain/kotlin/    # Shared Kotlin code for all platforms
├── commonTest/kotlin/    # Shared tests for all platforms
└── jvmTest/kotlin/       # JVM-specific tests (oracle-based validation)
```

## Supported JSON Schema features

- Basic types: string, number, boolean, object, array
- String constraints: `minLength`, `maxLength`, `pattern`
- Number constraints: `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`
- Objects: required/optional properties
- Arrays: item schemas
- Enumerations
- Discriminated unions (sum types)
- Path-aware validation errors
- JSON Schema export and import

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc.
via [GitHub](https://github.com/parttimenerd/femtoschema/issues) issues.
Contribution and feedback are encouraged and always welcome.

## License

MIT, Copyright 2026 Johannes Bechberger and contributors
