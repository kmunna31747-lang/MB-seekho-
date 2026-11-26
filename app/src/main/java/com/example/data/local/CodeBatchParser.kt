package com.example.data.local

/**
 * Streaming parser for CSV and TXT Access Code files (supports 1,000, 10,000, 50,000, 100,000+ codes).
 *
 * Supported formats per line:
 * 1. Numbered list format: `00001. MBST-XXXX-XXXX-XXXX` or `00001) MBST-XXXX-XXXX`
 * 2. Two-column whitespace/tab separated: `STU0001  MBST-XXXX-XXXX`
 * 3. Two-column CSV: `STU0001,MBST-XXXX-XXXX` or `"STU0001","MBST-XXXX-XXXX-XXXX"`
 * 4. Multi-column CSV where code is in any column (e.g. `STU0001,MBST-XXXX-XXXX,unused`)
 * 5. Single code per line: `MBST-XXXX-XXXX` or `MBST-XXXX-XXXX-XXXX`
 *
 * Automatically skips title headers, CSV header rows, comments (`#`), and blank lines.
 */
object CodeBatchParser {

    data class ParsedCodeEntry(
        val suggestedCodeId: String?,
        val normalizedRawCode: String
    )

    sealed interface LineParseResult {
        data object Ignored : LineParseResult
        data object Invalid : LineParseResult
        data class Parsed(val entry: ParsedCodeEntry) : LineParseResult
    }

    private val CODE_ID_REGEX = Regex("^(STU|TCH)[0-9]{3,10}$")
    private val NUMBERED_PREFIX_REGEX = Regex("^#?([0-9]{1,10})[.):]?$")
    private val ACCESS_CODE_REGEX = Regex("^[A-Z0-9][A-Z0-9\\-]{6,62}[A-Z0-9]$")
    private val PREFIXED_CODE_REGEX = Regex("^(MBST|MBTC)(-[A-Z0-9]{3,8}){2,5}$")

    fun parseLine(rawLine: String): LineParseResult {
        val trimmed = rawLine.trim().trimStart('\uFEFF')
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
            return LineParseResult.Ignored
        }

        val upper = trimmed.uppercase()
        // Ignore common document titles / headers
        if (upper.startsWith("MB SEEKHO") ||
            upper.startsWith("PREVIOUS ") ||
            upper.startsWith("CODEID") ||
            upper.startsWith("CODE_ID") ||
            upper.startsWith("ID,") ||
            upper.startsWith("SERIAL") ||
            upper.startsWith("ACCESS_CODE") ||
            upper.startsWith("ACCESS CODE") ||
            upper.startsWith("STUDENT_CODE") ||
            upper.startsWith("STUDENT ACCESS") ||
            upper.startsWith("BATCH")
        ) {
            return LineParseResult.Ignored
        }

        // Tokenize CSV or whitespace/tab separated columns
        val tokens = if (trimmed.contains(',')) {
            trimmed.split(',').map { it.trim().trim('"').trim('\'').uppercase() }.filter { it.isNotEmpty() }
        } else {
            trimmed.split(Regex("\\s+")).map { it.trim().trim('"').trim('\'').uppercase() }.filter { it.isNotEmpty() }
        }

        if (tokens.isEmpty()) return LineParseResult.Ignored

        // 1. Prefer matching an explicit MBST-/MBTC- formatted token (supports 2 to 5 segments)
        val prefixedToken = tokens.firstOrNull { PREFIXED_CODE_REGEX.matches(it) }
        if (prefixedToken != null) {
            val idToken = tokens.firstOrNull { it != prefixedToken && CODE_ID_REGEX.matches(it) }
                ?: deriveIdFromNumberedPrefix(tokens, prefixedToken)
            return LineParseResult.Parsed(
                ParsedCodeEntry(
                    suggestedCodeId = idToken,
                    normalizedRawCode = prefixedToken
                )
            )
        }

        // 2. Two-column format: [CodeId, AccessCode]
        if (tokens.size >= 2 && CODE_ID_REGEX.matches(tokens[0]) && isValidGenericCode(tokens[1])) {
            return LineParseResult.Parsed(
                ParsedCodeEntry(
                    suggestedCodeId = tokens[0],
                    normalizedRawCode = tokens[1]
                )
            )
        }

        // 3. Numbered list format with generic code: [00001., ACCESS-CODE]
        if (tokens.size >= 2 && NUMBERED_PREFIX_REGEX.matches(tokens[0]) && isValidGenericCode(tokens[1])) {
            return LineParseResult.Parsed(
                ParsedCodeEntry(
                    suggestedCodeId = deriveIdFromNumberedPrefix(tokens, tokens[1]),
                    normalizedRawCode = tokens[1]
                )
            )
        }

        // 4. Single-column raw access code
        if (tokens.size == 1 && isValidGenericCode(tokens[0]) && !CODE_ID_REGEX.matches(tokens[0])) {
            return LineParseResult.Parsed(
                ParsedCodeEntry(
                    suggestedCodeId = null,
                    normalizedRawCode = tokens[0]
                )
            )
        }

        return LineParseResult.Invalid
    }

    private fun deriveIdFromNumberedPrefix(tokens: List<String>, codeToken: String): String? {
        val numberToken = tokens.firstOrNull { it != codeToken && NUMBERED_PREFIX_REGEX.matches(it) } ?: return null
        val digits = NUMBERED_PREFIX_REGEX.matchEntire(numberToken)?.groupValues?.getOrNull(1) ?: return null
        val prefix = when {
            codeToken.startsWith("MBTC-") -> "TCH"
            codeToken.startsWith("MBST-") -> "STU"
            else -> return null
        }
        val minWidth = if (prefix == "STU") 5 else 3
        return "$prefix${digits.padStart(minWidth, '0')}"
    }

    private fun isValidGenericCode(token: String): Boolean {
        if (token.length !in 8..64) return false
        if (!ACCESS_CODE_REGEX.matches(token)) return false
        // Require at least one digit or hyphen so random English words in headers aren't misread as codes
        return token.any { it.isDigit() } || token.contains('-')
    }
}
