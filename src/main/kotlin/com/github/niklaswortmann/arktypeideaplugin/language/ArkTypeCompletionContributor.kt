package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.codeInsight.AutoPopupController
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.dialects.TypeScriptLanguageDialect
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import java.util.regex.Pattern

/**
 * Completion contributor for ArkType language.
 * Provides code completion suggestions for ArkType keywords and subtypes.
 * 
 * Features:
 * - Suggests TypeScript types and ArkType keywords at the beginning of type expressions
 * - Suggests appropriate subtypes after a dot based on the parent type
 * - Automatically adds a dot and triggers completion again when selecting a type that has subtypes
 * - Suggests custom type aliases defined within the same scope
 * 
 * The keywords and subtype hierarchy are defined in the companion object and can be extended
 * as needed to support more ArkType keywords and subtypes.
 * 
 * Based on the keywords description from keywords.md, this implementation supports:
 * - TypeScript keywords (string, number, boolean, etc.)
 * - ArkType utility types (Record, Partial, Required, etc.)
 * - Subtype keywords for refining types (date, iso, parse, etc.)
 * - Custom type aliases defined within the same scope
 */
class ArkTypeCompletionContributor : CompletionContributor() {
    companion object {
        // TypeScript keywords (excluding 'any' and 'void')
        private val TS_KEYWORDS = listOf(
            "string", "number", "boolean", "object", "array", "Date", "null", "undefined"
        )

        // Subtype keywords
        private val SUBTYPE_KEYWORDS = listOf(
            "date", "iso", "parse", "root"
        )

        // Map of parent types to their valid subtypes
        private val SUBTYPE_HIERARCHY = mapOf(
            // String subtypes
            "string" to listOf("date"),
            "string.date" to listOf("iso", "parse"),
            "string.date.iso" to listOf("parse"),

            // Number subtypes - adding common numeric validations
            "number" to listOf("integer", "positive", "negative", "min", "max", "range"),

            // Boolean subtypes
            "boolean" to listOf("true", "false"),

            // Object subtypes
            "object" to listOf("keys", "values", "entries"),

            // Array subtypes
            "array" to listOf("min", "max", "length", "items"),

            // Date subtypes
            "Date" to listOf("min", "max", "range")
        )

        // Keywords that can be used as root types
        private val ROOT_TYPES = TS_KEYWORDS + listOf(
            "Record", "Partial", "Required", "Pick", "Omit", "Exclude", "Extract"
        )
    }

    init {
        // Add completion for base TypeScript keywords
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(ArkTypeLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // Get the current text before the caret
                    val position = parameters.position
                    val text = position.text
                    val prefix = result.prefixMatcher.prefix

                    // If we're at the start of a type expression or after a non-dot character,
                    // suggest base TypeScript keywords and other root types
                    // TODO Check with complex expression
                    if (!prefix.contains(".")) {
                        // Add standard keywords
                        for (keyword in ROOT_TYPES) {
                            result.addElement(
                                LookupElementBuilder.create(keyword)
                                    .withPresentableText(keyword)
                                    .withTypeText(if (TS_KEYWORDS.contains(keyword)) "TypeScript type" else "ArkType keyword")
                                    .withIcon(ArkTypeFileType.INSTANCE.icon)
//                                    .withInsertHandler { insertionContext, item ->
//                                        // If we're in a position where we might want to add subtypes,
//                                        // add a dot to enable chaining
//                                        if (SUBTYPE_HIERARCHY.containsKey(keyword)) {
//                                            val editor = insertionContext.editor
//                                            val document = editor.document
//                                            val offset = insertionContext.tailOffset
//                                            document.insertString(offset, ".")
//                                            editor.caretModel.moveToOffset(offset + 1)
//                                            // Trigger completion again to show subtypes
//                                            AutoPopupController.getInstance(insertionContext.project)
//                                                .scheduleAutoPopup(editor)
//                                        }
//                                    }
                            )
                        }

                        // Add type aliases from the current scope
                        val file = parameters.originalFile
                        val offset = parameters.offset
                        val typeAliases = extractTypeAliasesFromScope(file, offset)

                        for (alias in typeAliases) {
                            result.addElement(
                                LookupElementBuilder.create(alias)
                                    .withPresentableText(alias)
                                    .withTypeText("Type alias")
                                    .withIcon(ArkTypeFileType.INSTANCE.icon)
                            )
                        }
                    }

                    // If we're after a dot, suggest appropriate subtypes based on the parent type
                    val currentText = getCurrentTypeExpression(parameters)
                    if (currentText.contains(".")) {
                        // Find the parent type to determine which subtypes to suggest
                        val parentType = currentText.substringBeforeLast(".")
                        val subtypes = SUBTYPE_HIERARCHY[parentType] ?: emptyList()
                        // TODO FIX ME
                        for (subtype in subtypes) {
                            result.addElement(
                                LookupElementBuilder.create(subtype)
                                    .withPresentableText(subtype)
                                    .withTypeText("Subtype of $parentType")
                                    .withIcon(ArkTypeFileType.INSTANCE.icon)
                                    .withInsertHandler { insertionContext, item ->
                                        // If this subtype can have further subtypes, add a dot
                                        val newType = "$parentType.$subtype"
                                        if (SUBTYPE_HIERARCHY.containsKey(newType)) {
                                            val editor = insertionContext.editor
                                            val document = editor.document
                                            val offset = insertionContext.tailOffset
                                            document.insertString(offset, ".")
                                            editor.caretModel.moveToOffset(offset + 1)
                                            // Trigger completion again to show subtypes
                                            AutoPopupController.getInstance(insertionContext.project)
                                                .scheduleAutoPopup(editor)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        )
    }

    /**
     * Extracts the current type expression from the completion parameters.
     * This helps determine the context for subtype suggestions.
     */
    private fun getCurrentTypeExpression(parameters: CompletionParameters): String {
        val position = parameters.position
        val file = position.containingFile
        val text = file.text
        val offset = parameters.offset

        // Find the start of the current type expression
        var startOffset = offset
        while (startOffset > 0) {
            val char = text[startOffset - 1]
            if (char == ' ' || char == '"' || char == '\'' || char == '`' || 
                char == ',' || char == '{' || char == '[' || char == '(') {
                break
            }
            startOffset--
        }

        return text.substring(startOffset, offset)
    }

    /**
     * Extracts type aliases from the current scope in the file.
     * This helps provide code completion for custom type aliases defined within the same scope.
     */
    private fun extractTypeAliasesFromScope(file: PsiFile, offset: Int): List<String> {
        // Get the original file if this is an injected file
        val injectedLanguageManager = InjectedLanguageManager.getInstance(file.project)
        val originalFile = injectedLanguageManager.getTopLevelFile(file)

        // Convert the offset from the injected file to the original file
        val originalOffset = if (file != originalFile) {
            val hostElement = injectedLanguageManager.getInjectionHost(file)
            if (hostElement != null) {
                val hostRange = injectedLanguageManager.injectedToHost(file, TextRange(offset, offset))
                hostRange?.startOffset ?: offset
            } else {
                offset
            }
        } else {
            offset
        }

        val text = originalFile.text
        val aliases = mutableListOf<String>()

        // Find all scope declarations in the file
        // A scope is typically defined as: const scopeName = scope({ ... }) or const scopeName = type.scope({ ... })
        val scopePattern = "(const|let|var)\\s+(\\w+)\\s*=\\s*(scope|type\\.scope)\\s*\\(\\s*\\{"
        val scopeRegex = Pattern.compile(scopePattern)
        val matcher = scopeRegex.matcher(text)

        while (matcher.find()) {
            val scopeStart = matcher.end()
            val scopeName = matcher.group(2)

            // Find the end of this scope by matching braces
            var openBraces = 1
            var scopeEnd = scopeStart
            while (openBraces > 0 && scopeEnd < text.length) {
                when (text[scopeEnd]) {
                    '{' -> openBraces++
                    '}' -> openBraces--
                }
                scopeEnd++
            }

            // Check if the current offset is within this scope or in a type definition that references this scope
            val scopeContent = text.substring(scopeStart, scopeEnd)

            // If we're within the scope declaration itself
            if (originalOffset >= scopeStart && originalOffset <= scopeEnd) {
                // Extract type aliases from this scope
                extractAliasesFromScopeContent(scopeContent, aliases)
                break
            }

            // If we're in a type definition that references this scope
            // e.g., const myType = coolScope.type({ ... })
            val typeDefPattern = "const\\s+\\w+\\s*=\\s*" + Pattern.quote(scopeName) + "\\.type\\s*\\(\\s*\\{"
            val typeDefRegex = Pattern.compile(typeDefPattern)
            val typeDefMatcher = typeDefRegex.matcher(text)

            while (typeDefMatcher.find()) {
                val typeDefStart = typeDefMatcher.end()

                // Find the end of this type definition
                openBraces = 1
                var typeDefEnd = typeDefStart
                while (openBraces > 0 && typeDefEnd < text.length) {
                    when (text[typeDefEnd]) {
                        '{' -> openBraces++
                        '}' -> openBraces--
                    }
                    typeDefEnd++
                }

                // Check if the current offset is within this type definition
                if (originalOffset >= typeDefStart && originalOffset <= typeDefEnd) {
                    // Extract type aliases from the referenced scope
                    extractAliasesFromScopeContent(scopeContent, aliases)
                    break
                }
            }
        }

        return aliases
    }

    /**
     * Extracts type aliases from the content of a scope.
     */
    private fun extractAliasesFromScopeContent(scopeContent: String, aliases: MutableList<String>) {
        // Type aliases are typically defined as: AliasName: "definition" or AliasName: { ... }
        val aliasPattern = "\\b(\\w+)\\s*:"
        val aliasRegex = Pattern.compile(aliasPattern)
        val aliasMatcher = aliasRegex.matcher(scopeContent)

        while (aliasMatcher.find()) {
            val aliasName = aliasMatcher.group(1)

            // Skip private aliases (those starting with #) and common keywords
            if (!aliasName.startsWith("#") && 
                !aliasName.equals("const") && 
                !aliasName.equals("let") && 
                !aliasName.equals("var") &&
                !ArkTypeSyntaxHighlighter.KEYWORDS.contains(aliasName) &&
                !ArkTypeSyntaxHighlighter.SUBTYPES.contains(aliasName)) {
                aliases.add(aliasName)
            }
        }
    }
}
