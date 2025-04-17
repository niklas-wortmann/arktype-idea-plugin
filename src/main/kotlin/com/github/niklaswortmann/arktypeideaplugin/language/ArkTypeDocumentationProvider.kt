package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.JSArgumentList
import java.util.regex.Pattern

/**
 * Documentation provider for ArkType keywords.
 * Provides quick documentation for ArkType keywords and subtypes.
 */
class ArkTypeDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element !is JSLiteralExpression || element !is JSArgumentList) return null

        // Check if the element is inside an ArkType function call or chained method
        if (!isInsideArkTypeDefinition(element) && !isInsideArkTypeChainedMethod(element)) {
            return null
        }

        // Check if the element is a string literal
        if (!element.isStringLiteral) return null

        // Get the text without quotes
        val text = element.text.let {
            if (it.length >= 2) it.substring(1, it.length - 1) else it
        }

        // Check if the text is a keyword or subtype
        if (ArkTypeSyntaxHighlighter.KEYWORDS.contains(text)) {
            return generateKeywordDoc(text)
        } else if (ArkTypeSyntaxHighlighter.SUBTYPES.contains(text)) {
            return generateSubtypeDoc(text)
        }

        return null
    }

    private fun isInsideArkTypeDefinition(element: PsiElement): Boolean {
        // Check if we're inside a call to one of the ArkType definition functions
        var current: PsiElement? = element.parent

        while (current != null) {
            if (current is JSCallExpression) {
                val methodExpression = current.methodExpression
                if (methodExpression is JSReferenceExpression) {
                    val methodName = methodExpression.referenceName ?: ""

                    // Match the arkDefinition pattern from the TextMate grammar
                    // Functions: type, generic, scope, define, match, fn, module, or any function starting with "ark"
                    if (methodName.matches(Regex("(type|generic|scope|define|match|fn|module|[aA]rk[a-zA-Z]*)"))) {
                        return true
                    }
                }
            }
            current = current.parent
        }
        return false
    }

    private fun isInsideArkTypeChainedMethod(element: PsiElement): Boolean {
        // Check if we're inside a call to one of the ArkType chained methods
        var current: PsiElement? = element.parent

        while (current != null) {
            if (current is JSCallExpression) {
                val methodExpression = current.methodExpression
                if (methodExpression is JSReferenceExpression) {
                    val qualifier = methodExpression.qualifier
                    val methodName = methodExpression.referenceName ?: ""

                    // Match the arkChained pattern from the TextMate grammar
                    // Chained methods: and, or, case, in, extends, ifExtends, intersect, merge, exclude, extract, overlaps, subsumes, to, satisfies
                    if (qualifier != null && methodName.matches(Regex("(and|or|case|in|extends|ifExtends|intersect|merge|exclude|extract|overlaps|subsumes|to|satisfies)"))) {
                        return true
                    }
                }
            }
            current = current.parent
        }
        return false
    }

    /**
     * Generates documentation for ArkType keywords.
     */
    fun generateKeywordDoc(keyword: String): String {
        val definition = when (keyword) {
            // TypeScript keywords
            "string" -> "Validates that a value is a string."
            "number" -> "Validates that a value is a number."
            "boolean" -> "Validates that a value is a boolean."
            "object" -> "Validates that a value is an object."
            "array" -> "Validates that a value is an array."
            "Date" -> "Validates that a value is a Date object."
            "null" -> "Validates that a value is null."
            "undefined" -> "Validates that a value is undefined."

            // ArkType utility types
            "Record" -> "Creates a type with specified keys and values."
            "Partial" -> "Makes all properties in a type optional."
            "Required" -> "Makes all properties in a type required."
            "Pick" -> "Constructs a type by picking the specified properties from a type."
            "Omit" -> "Constructs a type by omitting the specified properties from a type."
            "Exclude" -> "Constructs a type by excluding from a union type all types that are assignable to the specified type."
            "Extract" -> "Constructs a type by extracting from a union type all types that are assignable to the specified type."

            else -> "ArkType keyword: $keyword"
        }

        return DocumentationMarkup.DEFINITION_START +
                keyword +
                DocumentationMarkup.DEFINITION_END +
                DocumentationMarkup.CONTENT_START +
                definition +
                DocumentationMarkup.CONTENT_END
    }

    /**
     * Generates documentation for ArkType subtypes.
     */
    fun generateSubtypeDoc(subtype: String): String {
        val definition = when (subtype) {
            // Subtype keywords
            "date" -> "Validates that a string is a valid date format."
            "iso" -> "Validates that a string is in ISO date format."
            "parse" -> "Transforms a string into a Date object."
            "root" -> "Gets the base type of a subtyped module."

            // Number subtypes
            "integer" -> "Validates that a number is an integer."
            "positive" -> "Validates that a number is positive."
            "negative" -> "Validates that a number is negative."
            "min" -> "Validates that a number is greater than or equal to a minimum value."
            "max" -> "Validates that a number is less than or equal to a maximum value."
            "range" -> "Validates that a number is within a specified range."

            // Boolean subtypes
            "true" -> "Validates that a boolean is true."
            "false" -> "Validates that a boolean is false."

            // Object subtypes
            "keys" -> "Gets the keys of an object type."
            "values" -> "Gets the values of an object type."
            "entries" -> "Gets the entries of an object type."

            // Array subtypes
            "length" -> "Validates the length of an array."
            "items" -> "Validates the items of an array."

            else -> "ArkType subtype: $subtype"
        }

        return DocumentationMarkup.DEFINITION_START +
                subtype +
                DocumentationMarkup.DEFINITION_END +
                DocumentationMarkup.CONTENT_START +
                definition +
                DocumentationMarkup.CONTENT_END
    }
}
