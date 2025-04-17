package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Syntax highlighter for ArkType language.
 * Defines how different elements of the language are highlighted in the editor.
 */
class ArkTypeSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        // Define text attribute keys for different token types
        val KEYWORD = TextAttributesKey.createTextAttributesKey("ARKTYPE.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val SUBTYPE = TextAttributesKey.createTextAttributesKey("ARKTYPE.SUBTYPE", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val STRING = TextAttributesKey.createTextAttributesKey("ARKTYPE.STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = TextAttributesKey.createTextAttributesKey("ARKTYPE.NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val OPERATOR = TextAttributesKey.createTextAttributesKey("ARKTYPE.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val IDENTIFIER = TextAttributesKey.createTextAttributesKey("ARKTYPE.IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val DOT = TextAttributesKey.createTextAttributesKey("ARKTYPE.DOT", DefaultLanguageHighlighterColors.DOT)
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("ARKTYPE.BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        // Define the keywords for ArkType (excluding subtypes)
        val KEYWORDS = setOf(
            // TypeScript keywords (excluding 'any' and 'void')
            "string", "number", "boolean", "object", "array", "Date", "null", "undefined",

            // ArkType utility types
            "Record", "Partial", "Required", "Pick", "Omit", "Exclude", "Extract"
        )

        // Define the subtypes for ArkType (to be highlighted in blue)
        val SUBTYPES = setOf(
            // Subtype keywords
            "date", "iso", "parse", "root",

            // Number subtypes
            "integer", "positive", "negative", "min", "max", "range",

            // Boolean subtypes
            "true", "false",

            // Object subtypes
            "keys", "values", "entries",

            // Array subtypes
            "length", "items"
        )

    }

    override fun getHighlightingLexer(): Lexer {
        return ArkTypeLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            ArkTypeTokenType.KEYWORD -> arrayOf(KEYWORD)
            ArkTypeTokenType.SUBTYPE -> arrayOf(SUBTYPE)
            ArkTypeTokenType.STRING -> arrayOf(STRING)
            ArkTypeTokenType.NUMBER -> arrayOf(NUMBER)
            ArkTypeTokenType.OPERATOR -> arrayOf(OPERATOR)
            ArkTypeTokenType.IDENTIFIER -> arrayOf(IDENTIFIER)
            ArkTypeTokenType.DOT -> arrayOf(DOT)
            TokenType.BAD_CHARACTER -> arrayOf(BAD_CHARACTER)
            else -> TextAttributesKey.EMPTY_ARRAY
        }
    }
}

/**
 * Token types for ArkType language.
 */
class ArkTypeTokenType(debugName: String) : IElementType(debugName, ArkTypeLanguage.INSTANCE) {
    companion object {
        val KEYWORD = ArkTypeTokenType("KEYWORD")
        val SUBTYPE = ArkTypeTokenType("SUBTYPE")
        val STRING = ArkTypeTokenType("STRING")
        val NUMBER = ArkTypeTokenType("NUMBER")
        val OPERATOR = ArkTypeTokenType("OPERATOR")
        val IDENTIFIER = ArkTypeTokenType("IDENTIFIER")
        val DOT = ArkTypeTokenType("DOT")
    }
}
