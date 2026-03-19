package me.bechberger.util.femtoschema.json

/**
 * Minimal JSON parser: parses a JSON string into Map/List/String/Number/Boolean/null.
 */
internal object JsonParser {

    fun parse(json: String): Any? {
        val state = ParserState(json)
        val result = state.parseValue()
        state.skipWhitespace()
        if (state.pos < state.input.length) {
            throw IllegalArgumentException("Unexpected trailing content at position ${state.pos}")
        }
        return result
    }

    private class ParserState(val input: String) {
        var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) throw IllegalArgumentException("Unexpected end of input")
            return when (input[pos]) {
                '"' -> parseString()
                '{' -> parseObject()
                '[' -> parseArray()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        fun skipWhitespace() {
            while (pos < input.length && input[pos] in " \t\n\r") pos++
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val c = input[pos++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (pos >= input.length) throw IllegalArgumentException("Unexpected end of string escape")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) throw IllegalArgumentException("Invalid unicode escape")
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Invalid escape character: $esc")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val map = linkedMapOf<String, Any?>()
            if (pos < input.length && input[pos] == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos >= input.length) throw IllegalArgumentException("Unterminated object")
                when (input[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at position $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val list = mutableListOf<Any?>()
            if (pos < input.length && input[pos] == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                if (pos >= input.length) throw IllegalArgumentException("Unterminated array")
                when (input[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at position $pos")
                }
            }
        }

        private fun parseBoolean(): Boolean {
            return if (input.startsWith("true", pos)) {
                pos += 4; true
            } else if (input.startsWith("false", pos)) {
                pos += 5; false
            } else {
                throw IllegalArgumentException("Expected boolean at position $pos")
            }
        }

        private fun parseNull(): Nothing? {
            if (input.startsWith("null", pos)) {
                pos += 4; return null
            }
            throw IllegalArgumentException("Expected null at position $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (pos < input.length && input[pos] == '-') pos++
            if (pos >= input.length || !input[pos].isDigit()) {
                throw IllegalArgumentException("Invalid number at position $start")
            }
            // integer part
            if (input[pos] == '0') {
                pos++
            } else {
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            var isDouble = false
            // fraction
            if (pos < input.length && input[pos] == '.') {
                isDouble = true
                pos++
                if (pos >= input.length || !input[pos].isDigit()) {
                    throw IllegalArgumentException("Invalid number at position $start")
                }
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            // exponent
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                isDouble = true
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                if (pos >= input.length || !input[pos].isDigit()) {
                    throw IllegalArgumentException("Invalid number at position $start")
                }
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            val numStr = input.substring(start, pos)
            return if (isDouble) {
                numStr.toDouble()
            } else {
                val longVal = numStr.toLong()
                if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) longVal.toInt() else longVal
            }
        }

        private fun expect(c: Char) {
            if (pos >= input.length || input[pos] != c) {
                throw IllegalArgumentException("Expected '$c' at position $pos")
            }
            pos++
        }
    }
}
