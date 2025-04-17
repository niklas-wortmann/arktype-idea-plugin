package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.*
import java.util.regex.Pattern

/**
 * Reference contributor for ArkType language.
 * Provides reference resolution for custom type aliases defined within an ArkType scope.
 */
class ArkTypeReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for identifiers in ArkType language
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(ArkTypeLanguage.INSTANCE),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    // Process identifier tokens, subtypes, and string literals
                    if (element.node.elementType != ArkTypeTokenType.IDENTIFIER && 
                        element.node.elementType != ArkTypeTokenType.STRING &&
                        element.node.elementType != ArkTypeTokenType.SUBTYPE) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val text = element.text
                    // Skip empty elements
                    if (text.isEmpty()) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    // For string literals, we need to extract potential type references
                    if (element.node.elementType == ArkTypeTokenType.STRING) {
                        // Remove quotes
                        val content = text.substring(1, text.length - 1)

                        // Extract potential type references from the string content
                        val references = mutableListOf<PsiReference>()

                        // Split by common type separators
                        val parts = content.split("|", "&", ",", " ", "<", ">", "(", ")", "[", "]", ".")

                        for (part in parts) {
                            val trimmed = part.trim()
                            if (trimmed.isNotEmpty() && Character.isJavaIdentifierStart(trimmed[0])) {
                                // Create a reference for each potential type name
                                val startOffset = text.indexOf(trimmed) + 1 // +1 for the opening quote
                                if (startOffset > 0) { // Ensure we found a valid offset
                                    references.add(ArkTypeReference(element, TextRange(startOffset, startOffset + trimmed.length)))
                                }
                            }
                        }

                        // Also look for specific patterns like "Id[]" to extract "Id"
                        val arrayPattern = Regex("([a-zA-Z_][a-zA-Z0-9_]*)\\[\\]")
                        val arrayMatches = arrayPattern.findAll(content)
                        for (match in arrayMatches) {
                            val typeName = match.groupValues[1]
                            val startOffset = text.indexOf(typeName) + 1 // +1 for the opening quote
                            if (startOffset > 0 && !references.any { it.rangeInElement.startOffset == startOffset }) {
                                references.add(ArkTypeReference(element, TextRange(startOffset, startOffset + typeName.length)))
                            }
                        }

                        return references.toTypedArray()
                    }

                    // For identifiers, create a reference for the whole text
                    return arrayOf(ArkTypeReference(element, TextRange(0, text.length)))
                }
            }
        )
    }
}

/**
 * Reference implementation for ArkType custom type aliases.
 * Resolves references to their declarations within the same scope.
 */
class ArkTypeReference(element: PsiElement, rangeInElement: TextRange) : PsiReferenceBase<PsiElement>(element, rangeInElement) {
    override fun resolve(): PsiElement? {
        // Get the text within the range of this reference
        val referenceName = if (element.node.elementType == ArkTypeTokenType.STRING || element.node.elementType.toString() == "JS:STRING_LITERAL") {
            // For string literals, extract the text from the range
            val rangeText = element.text.substring(rangeInElement.startOffset, rangeInElement.endOffset)

            // Handle array type references (e.g., "Id[]")
            val arrayPattern = Regex("([a-zA-Z_][a-zA-Z0-9_]*)\\[\\]")
            val arrayMatch = arrayPattern.find(rangeText)
            if (arrayMatch != null) {
                arrayMatch.groupValues[1].trim()
            } else {
                rangeText.trim()
            }
        } else {
            // For identifiers, use the whole text
            element.text
        }

        println("[DEBUG_LOG] Resolving reference: $referenceName")

        // First, try to find the declaration in the TypeScript part
        val tsDeclaration = findTypeScriptDeclaration(referenceName)
        if (tsDeclaration != null) {
            println("[DEBUG_LOG] Found TypeScript declaration: ${tsDeclaration.text}")
            return tsDeclaration
        }

        // If not found in TypeScript, fall back to the current approach
        // Find all scope elements in the file
        val scopeElements = findAllScopeElements(element.containingFile)
        println("[DEBUG_LOG] Found ${scopeElements.size} scope elements")

        // Look for a declaration of this type alias within any scope
        for (scopeElement in scopeElements) {
            println("[DEBUG_LOG] Searching in scope element: ${scopeElement.text.take(50)}...")
            val declaration = findDeclaration(scopeElement, referenceName)
            if (declaration != null) {
                println("[DEBUG_LOG] Found declaration: ${declaration.text}")
                return declaration
            }
        }

        println("[DEBUG_LOG] No declaration found for $referenceName")

        return null
    }

    /**
     * Find the declaration of a type alias in the TypeScript part of the file.
     * This allows jumping from the ArkType language section in the value to the TypeScript language part in the key definition.
     */
    private fun findTypeScriptDeclaration(referenceName: String): PsiElement? {
        // Get the injected language manager
        val injectedLanguageManager = InjectedLanguageManager.getInstance(element.project)

        // Get the host file (TypeScript file)
        val hostFile = injectedLanguageManager.getInjectionHost(element.containingFile)?.containingFile
            ?: return null

        println("[DEBUG_LOG] Host file: ${hostFile.name}")

        // Convert the offset from the injected file to the host file
        val hostOffset = injectedLanguageManager.injectedToHost(
            element.containingFile, 
            TextRange(element.textRange.startOffset, element.textRange.startOffset)
        )?.startOffset ?: return null

        println("[DEBUG_LOG] Host offset: $hostOffset")

        // Find all property assignments in the host file that match the reference name
        val propertyAssignments = findPropertyAssignmentsInFile(hostFile, referenceName)

        // If no property assignments found, return null
        if (propertyAssignments.isEmpty()) {
            println("[DEBUG_LOG] No property assignments found for $referenceName")
            return null
        }

        println("[DEBUG_LOG] Found ${propertyAssignments.size} property assignments for $referenceName")

        // Find the property assignment that is closest to the host offset
        // This is a simple heuristic to find the most relevant declaration
        var closestAssignment: PsiElement? = null
        var minDistance = Int.MAX_VALUE

        for (assignment in propertyAssignments) {
            val distance = Math.abs(assignment.textRange.startOffset - hostOffset)
            if (distance < minDistance) {
                minDistance = distance
                closestAssignment = assignment
            }
        }

        return closestAssignment
    }

    /**
     * Find all property assignments in the file that match the reference name.
     */
    private fun findPropertyAssignmentsInFile(file: PsiFile, referenceName: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()

        // Recursively search for property assignments
        findPropertyAssignmentsRecursively(file, referenceName, result)

        return result
    }

    /**
     * Recursively search for property assignments that match the reference name.
     */
    private fun findPropertyAssignmentsRecursively(element: PsiElement, referenceName: String, result: MutableList<PsiElement>) {
        // Check if this element is a property assignment with the reference name
        if (element is JSProperty && element.name == referenceName) {
            result.add(element)
        }

        // Recursively check all children
        for (child in element.children) {
            findPropertyAssignmentsRecursively(child, referenceName, result)
        }
    }

    /**
     * Finds all scope elements in the file.
     * This is used to collect all type aliases defined in any scope.
     */
    private fun findAllScopeElements(file: PsiFile): List<PsiElement> {
        val scopeElements = mutableListOf<PsiElement>()

        // Recursively search for all scope elements in the file
        findScopeElementsRecursively(file, scopeElements)

        return scopeElements
    }

    /**
     * Recursively searches for scope elements in the given element and its children.
     */
    private fun findScopeElementsRecursively(element: PsiElement, scopeElements: MutableList<PsiElement>) {
        // Check if this element is a scope element
        if (isLikelyScopeElement(element)) {
            scopeElements.add(element)
        }

        // Recursively check all children
        for (child in element.children) {
            findScopeElementsRecursively(child, scopeElements)
        }
    }

    /**
     * Find the scope element that contains all type definitions.
     * This is typically the object literal passed to the scope() function.
     */
    private fun findScopeElement(element: PsiElement): PsiElement? {
        // Start from the current element and traverse up to find the scope
        var current: PsiElement? = element

        while (current != null) {
            // If we've reached the root of the injected fragment, stop
            if (current is PsiFile) {
                break
            }

            // If we've found an object literal that's likely the scope, return it
            if (isLikelyScopeElement(current)) {
                return current
            }

            current = current.parent
        }

        return null
    }

    /**
     * Check if the element is likely to be a scope element (object literal passed to scope function).
     */
    private fun isLikelyScopeElement(element: PsiElement): Boolean {
        // Check if the element contains text with a pattern like "key: value"
        val text = element.text

        // Basic check for object literal syntax
        if (!text.contains(":") || !text.contains("{") || !text.contains("}")) {
            return false
        }

        // Check if this element is inside a scope() call
        var parent = element.parent
        var depth = 0
        while (parent != null && depth < 5) { // Limit depth to avoid infinite loops
            val parentText = parent.text
            if (parentText.contains("scope(") && parentText.contains(element.text)) {
                return true
            }
            parent = parent.parent
            depth++
        }

        // Fallback: check if the element looks like an object literal with type definitions
        // Look for a pattern of property assignments (key: value) that would indicate a scope object
        val propertyPattern = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*:")
        val matches = propertyPattern.findAll(text)
        return matches.count() > 0
    }

    /**
     * Find the declaration of a type alias within the scope element.
     */
    private fun findDeclaration(scopeElement: PsiElement, referenceName: String): PsiElement? {
        // First, try to find a direct property assignment with the reference name
        val directDeclaration = findDirectDeclaration(scopeElement, referenceName)
        if (directDeclaration != null) {
            return directDeclaration
        }

        // If not found, recursively search through all children of the scope element
        return findDeclarationInChildren(scopeElement, referenceName)
    }

    /**
     * Find a direct declaration of a type alias in the scope element.
     * This looks for patterns like "AliasName: ..." which is how type aliases are defined in ArkType.
     */
    private fun findDirectDeclaration(scopeElement: PsiElement, referenceName: String): PsiElement? {
        val text = scopeElement.text

        // Look for the pattern "referenceName: " which is how type aliases are defined
        val pattern = "\\b$referenceName\\s*:"
        val regex = Regex(pattern)
        val match = regex.find(text) ?: return null

        // Find the PsiElement at this position
        val startOffset = match.range.first + scopeElement.textRange.startOffset
        val element = scopeElement.containingFile.findElementAt(startOffset)

        return element
    }

    /**
     * Recursively search for a declaration in the children of an element.
     */
    private fun findDeclarationInChildren(element: PsiElement, referenceName: String): PsiElement? {
        // Check if this element is a declaration of the reference
        if (isDeclarationElement(element, referenceName)) {
            return element
        }

        // Recursively check all children
        for (child in element.children) {
            val result = findDeclarationInChildren(child, referenceName)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Check if the element is a declaration of the reference.
     */
    private fun isDeclarationElement(element: PsiElement, referenceName: String): Boolean {
        // Check if this element is a property name in an object literal
        // that matches the reference name

        // Get the text of the element
        val text = element.text.trim()

        // Check if the text matches the reference name exactly
        if (text != referenceName) {
            return false
        }

        // Check if this element is followed by a colon (property assignment)
        val nextSibling = element.nextSibling
        if (nextSibling != null) {
            val nextText = nextSibling.text.trim()
            if (nextText.startsWith(":")) {
                return true
            }
        }

        // Also check if this element is a key in an object literal
        // by looking at its parent and siblings
        val parent = element.parent
        if (parent != null) {
            // Check if the parent contains property assignments
            val parentText = parent.text
            if (parentText.contains(":") && parentText.contains("{") && parentText.contains("}")) {
                // Check if there's a colon after this element within the parent
                val elementEndOffset = element.textRange.endOffset - parent.textRange.startOffset
                if (elementEndOffset < parentText.length) {
                    val textAfterElement = parentText.substring(elementEndOffset).trim()
                    if (textAfterElement.startsWith(":")) {
                        return true
                    }
                }
            }
        }

        // Check if this element is a property name in a JavaScript object literal
        // This is a more specific check for the ArkType scope object
        if (element.parent?.text?.contains("${referenceName}:") == true) {
            return true
        }

        // Check if this element is inside a scope declaration
        // This is specific for ArkType type aliases
        var currentElement = element
        var depth = 0
        while (currentElement.parent != null && depth < 10) {
            currentElement = currentElement.parent
            depth++

            val currentText = currentElement.text
            if ((currentText.contains("scope({") || currentText.contains("type.scope({")) && 
                currentText.contains("$referenceName:")) {
                return true
            }
        }

        return false
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
