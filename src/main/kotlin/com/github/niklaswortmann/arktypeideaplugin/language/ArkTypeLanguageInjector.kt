package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.impl.JSLiteralExpressionImpl

/**
 * Language injector for ArkType expressions in JavaScript/TypeScript files.
 * Handles complex type expressions including:
 * - Array type expressions with bounds (e.g., "0 < string[] <= 10")
 * - Date expressions (e.g., "Date < d'2000-01-01'")
 * - Optional properties (e.g., "optionalKey?": "number[]")
 * - Computed property names (e.g., [symbolicKey]: "string?")
 * - Template literals (e.g., `Date < ${Date.now()}`)
 */
class ArkTypeLanguageInjector : MultiHostInjector {
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        // Return JSLiteralExpression to target string literals in JavaScript/TypeScript files
        return listOf(JSLiteralExpression::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        if (host !is JSLiteralExpression) {
            return
        }

        // Check if the element is in a JavaScript or TypeScript file
        val language = host.containingFile?.language?.id
        if (language != "JavaScript" && language != "TypeScript" && language != "TypeScript JSX" && language != "JavaScript JSX") {
            return
        }

        // Check if this string is inside an ArkType function call or chained method
        if (!isInsideArkTypeDefinition(host) && !isInsideArkTypeChainedMethod(host)) {
            return
        }

        // Handle different types of literals
        when (host) {
            is JSLiteralExpressionImpl -> {
                val text = host.text
                when {
                    // Handle regular string literals (single or double quotes)
                    host.isStringLiteral -> {
                        injectIntoStringLiteral(registrar, host)
                    }

                    // Handle template literals (backtick strings)
                    text.startsWith("`") && text.endsWith("`") -> {
                        injectIntoTemplateLiteral(registrar, host, text)
                    }
                }
            }
            else -> {
                // Handle other types of literals if needed
            }
        }
    }

    private fun injectIntoStringLiteral(registrar: MultiHostRegistrar, host: JSLiteralExpression) {
        val content = host.text
        // Skip if not a proper string literal
        if (content.length < 2) return

        // Remove quotes from the string content
        val unquotedContent = content.substring(1, content.length - 1)

        // Use our custom ArkType language for injection
        registrar.startInjecting(ArkTypeLanguage.INSTANCE)

        // For ArkType, we need a more flexible context that can handle all the complex expressions
        // Instead of trying to determine the specific type of expression, we'll use a more general approach
        // that can handle all the examples from the issue description
        val prefix = "type ArkType = "
        val suffix = ";"

        registrar.addPlace(
            prefix, 
            suffix, 
            host as PsiLanguageInjectionHost, 
            TextRange(1, content.length - 1)
        )

        registrar.doneInjecting()
    }

    private fun injectIntoTemplateLiteral(registrar: MultiHostRegistrar, host: JSLiteralExpression, content: String) {
        // Skip if not a proper template literal
        if (content.length < 2) return

        // Remove backticks from the template literal content
        val unquotedContent = content.substring(1, content.length - 1)

        // Use our custom ArkType language for injection
        registrar.startInjecting(ArkTypeLanguage.INSTANCE)

        // For ArkType, we need a more flexible context that can handle all the complex expressions
        // Use the same approach as for string literals for consistency
        val prefix = "type ArkType = "
        val suffix = ";"

        registrar.addPlace(
            prefix, 
            suffix, 
            host as PsiLanguageInjectionHost, 
            TextRange(1, content.length - 1)
        )

        registrar.doneInjecting()
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

}
