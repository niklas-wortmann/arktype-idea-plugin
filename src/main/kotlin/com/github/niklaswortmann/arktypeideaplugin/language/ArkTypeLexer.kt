package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for ArkType language.
 * Breaks down the input text into tokens that can be processed by the syntax highlighter.
 */
class ArkTypeLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var bufferStart: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentToken: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferStart = startOffset
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? {
        return if (tokenStart >= bufferEnd) null else currentToken
    }

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        if (tokenEnd >= bufferEnd) {
            tokenStart = bufferEnd
            tokenEnd = bufferEnd
            currentToken = null
            return
        }

        tokenStart = tokenEnd
        tokenEnd = tokenStart

        // Handle whitespace as a token
        if (tokenStart < bufferEnd && Character.isWhitespace(buffer[tokenStart])) {
            while (tokenEnd < bufferEnd && Character.isWhitespace(buffer[tokenEnd])) {
                tokenEnd++
            }
            currentToken = TokenType.WHITE_SPACE
            return
        }

        if (tokenStart >= bufferEnd) {
            tokenEnd = bufferEnd
            currentToken = null
            return
        }

        tokenEnd = tokenStart + 1

        val currentChar = buffer[tokenStart]

        when {
            currentChar == '.' -> {
                currentToken = ArkTypeTokenType.DOT
            }
            currentChar == '<' || currentChar == '>' || currentChar == '=' || 
            currentChar == '+' || currentChar == '-' || currentChar == '*' || 
            currentChar == '/' || currentChar == '?' || currentChar == '!' || 
            currentChar == '&' || currentChar == '|' || currentChar == '^' ||
            currentChar == '[' || currentChar == ']' ||
            currentChar == '{' || currentChar == '}' ||
            currentChar == '(' || currentChar == ')' ||
            currentChar == ',' || currentChar == ':' || currentChar == ';' -> {
                currentToken = ArkTypeTokenType.OPERATOR
            }
            currentChar == '"' || currentChar == '\'' || currentChar == '`' -> {
                // Handle string literals
                while (tokenEnd < bufferEnd && buffer[tokenEnd] != currentChar) {
                    if (buffer[tokenEnd] == '\\' && tokenEnd + 1 < bufferEnd) {
                        tokenEnd += 2
                    } else {
                        tokenEnd++
                    }
                }
                if (tokenEnd < bufferEnd) {
                    tokenEnd++
                }
                currentToken = ArkTypeTokenType.STRING
            }
            Character.isDigit(currentChar) -> {
                // Handle number literals
                while (tokenEnd < bufferEnd && 
                      (Character.isDigit(buffer[tokenEnd]) || 
                       buffer[tokenEnd] == '.' && tokenEnd + 1 < bufferEnd && Character.isDigit(buffer[tokenEnd + 1]))) {
                    tokenEnd++
                }
                currentToken = ArkTypeTokenType.NUMBER
            }
            Character.isJavaIdentifierStart(currentChar) -> {
                // Handle identifiers and keywords
                while (tokenEnd < bufferEnd && Character.isJavaIdentifierPart(buffer[tokenEnd])) {
                    tokenEnd++
                }

                val text = buffer.subSequence(tokenStart, tokenEnd).toString()
                currentToken = when {
                    ArkTypeSyntaxHighlighter.KEYWORDS.contains(text) -> ArkTypeTokenType.KEYWORD
                    ArkTypeSyntaxHighlighter.SUBTYPES.contains(text) -> ArkTypeTokenType.SUBTYPE
                    else -> ArkTypeTokenType.IDENTIFIER
                }
            }
            else -> {
                currentToken = TokenType.BAD_CHARACTER
            }
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd
}
