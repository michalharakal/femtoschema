package me.bechberger.util.femtoschema.json

/**
 * Minimal JSON pretty-printer: converts Map/List/String/Number/Boolean/null to a JSON string.
 */
internal object JsonPrinter {

    fun prettyPrint(value: Any?, indent: Int = 2): String {
        val sb = StringBuilder()
        append(sb, value, 0, indent)
        return sb.toString()
    }

    private fun append(sb: StringBuilder, value: Any?, level: Int, indent: Int) {
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(value)
            is Number -> appendNumber(sb, value)
            is String -> appendString(sb, value)
            is Map<*, *> -> appendObject(sb, value, level, indent)
            is List<*> -> appendArray(sb, value, level, indent)
            else -> appendString(sb, value.toString())
        }
    }

    private fun appendNumber(sb: StringBuilder, value: Number) {
        val d = value.toDouble()
        if (d == d.toLong().toDouble() && !d.isInfinite()) {
            sb.append(d.toLong())
        } else {
            sb.append(d)
        }
    }

    private fun appendString(sb: StringBuilder, value: String) {
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        sb.append("\\u")
                        sb.append(c.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append('"')
    }

    private fun appendObject(sb: StringBuilder, map: Map<*, *>, level: Int, indent: Int) {
        if (map.isEmpty()) {
            sb.append("{}")
            return
        }
        val pad = " ".repeat((level + 1) * indent)
        val endPad = " ".repeat(level * indent)
        sb.append("{\n")
        val entries = map.entries.toList()
        for ((i, entry) in entries.withIndex()) {
            sb.append(pad)
            appendString(sb, entry.key.toString())
            sb.append(": ")
            append(sb, entry.value, level + 1, indent)
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append(endPad).append("}")
    }

    private fun appendArray(sb: StringBuilder, list: List<*>, level: Int, indent: Int) {
        if (list.isEmpty()) {
            sb.append("[]")
            return
        }
        val pad = " ".repeat((level + 1) * indent)
        val endPad = " ".repeat(level * indent)
        sb.append("[\n")
        for ((i, item) in list.withIndex()) {
            sb.append(pad)
            append(sb, item, level + 1, indent)
            if (i < list.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append(endPad).append("]")
    }
}
