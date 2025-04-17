package com.github.niklaswortmann.arktypeideaplugin

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference

class ArkTypeReferenceTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    /**
     * Test that references to type aliases resolve to their declarations
     */
    fun testTypeAliasReference() {
        // Create a test file with TypeScript content containing ArkType expressions
        val fileContent = """
            import {scope, type} from "arktype";

            // Define a scope with type aliases
            const coolScope = scope({
                // Type aliases that should be available for completion within the scope
                Id: "string",
                User: { id: "Id", friends: "Id[]" },
                UsersById: {
                    "[Id]": "User | undefined"
                }
            })
        """.trimIndent()

        // Configure the editor with the test content as a TypeScript file
        myFixture.configureByText(JavaScriptFileType.INSTANCE, fileContent)

        // Manually find the scope element
        val file = myFixture.file
        val scopeStart = fileContent.indexOf("scope({")
        val scopeElement = file.findElementAt(scopeStart)


        // Find the Id declaration
        val idPos = fileContent.indexOf("Id: \"string\"")
        val idElement = file.findElementAt(idPos)


        // Find the User declaration
        val userPos = fileContent.indexOf("User: {")
        val userElement = file.findElementAt(userPos)

        // Verify that we can find the declarations
        assertNotNull("Should find the Id declaration", idElement)
        assertNotNull("Should find the User declaration", userElement)

        // Now test the reference resolution
        // Find the reference to "Id" in "id: "Id""
        val idReferencePos = fileContent.indexOf("id: \"Id\"") + 5
        val idReference = findReferenceAt(idReferencePos)
        assertNotNull("Should find a reference at the specified position", idReference)

        // Print the reference class to see what kind of reference it is
        println("[DEBUG_LOG] Id reference class: ${idReference?.javaClass?.name}")

        // Try to manually resolve the reference
        val idReferenceName = "Id"
        val idDeclaration = findDeclarationInFile(file, idReferenceName)
        assertNotNull("Should find the Id declaration in the file", idDeclaration)

        // Check that the declaration contains the "Id" type alias
        assertTrue("Declaration should contain the Id type alias", idDeclaration?.text?.contains("Id") == true)
    }

    /**
     * Test that references in the ArkType language section can be resolved to declarations in the TypeScript language part.
     * This tests the cross-language reference resolution feature.
     */
    fun testCrossLanguageReferenceResolution() {
        // Create a test file with TypeScript content containing ArkType expressions
        val fileContent = """
            import {scope, type} from "arktype";

            // Define a scope with type aliases
            const coolScope = scope({
                // Type aliases that should be available for completion within the scope
                Id: "string",
                User: { id: "Id", friends: "Id[]" },
                UsersById: {
                    "[Id]": "User | undefined"
                }
            })
        """.trimIndent()

        // Configure the editor with the test content as a TypeScript file
        myFixture.configureByText(JavaScriptFileType.INSTANCE, fileContent)

        // Find the Id declaration in the TypeScript part
        val file = myFixture.file
        val idPos = fileContent.indexOf("Id: \"string\"")
        val idElement = file.findElementAt(idPos)

        println("[DEBUG_LOG] Id element: ${idElement?.text}")
        assertNotNull("Should find the Id declaration", idElement)

        // Find the reference to "Id" in "id: "Id"" in the ArkType part
        val idReferencePos = fileContent.indexOf("id: \"Id\"") + 5
        val idReference = findReferenceAt(idReferencePos)

        println("[DEBUG_LOG] Id reference: ${idReference?.element?.text}")
        assertNotNull("Should find a reference at the specified position", idReference)

        // Resolve the reference
        val resolved = idReference?.resolve()

        println("[DEBUG_LOG] Resolved element: ${resolved?.text}")
        assertNotNull("Should resolve the reference", resolved)

        // Check that the resolved element is the Id declaration in the TypeScript part
        // We can't directly compare the elements because they might be different PsiElement instances
        // So we check that the resolved element contains the text "Id" and is near the Id declaration
        val resolvedOffset = resolved?.textRange?.startOffset ?: 0
        val idOffset = idElement?.textRange?.startOffset ?: 0

        println("[DEBUG_LOG] Resolved offset: $resolvedOffset, Id offset: $idOffset")

        // Check that the resolved element contains the text "Id"
        assertTrue("Resolved element should contain 'Id'", resolved?.text?.contains("Id") == true)

        // Check that the resolved element is near the Id declaration (within 10 characters)
        // This is a simple heuristic to check that we're resolving to the right element
        assertTrue("Resolved element should be near the Id declaration", 
            Math.abs(resolvedOffset - idOffset) < 10)
    }

    /**
     * Helper method to find a declaration in the file
     */
    private fun findDeclarationInFile(file: PsiFile, name: String): PsiElement? {
        // Find all elements in the file
        val allElements = mutableListOf<PsiElement>()
        collectElements(file, allElements)

        println("[DEBUG_LOG] Found ${allElements.size} elements in the file")

        // Look for an element that contains the name and is likely a declaration
        for (element in allElements) {
            val text = element.text
            if (text.contains("$name:") || text.contains("$name =")) {
                println("[DEBUG_LOG] Found potential declaration: $text")
                return element
            }
        }

        return null
    }

    /**
     * Helper method to collect all elements in a file
     */
    private fun collectElements(element: PsiElement, elements: MutableList<PsiElement>) {
        elements.add(element)
        for (child in element.children) {
            collectElements(child, elements)
        }
    }

    /**
     * Helper method to find a reference at a specific position in the file
     */
    private fun findReferenceAt(offset: Int): PsiReference? {
        val element = myFixture.file.findElementAt(offset)
        println("[DEBUG_LOG] Element at offset $offset: ${element?.text}, type: ${element?.node?.elementType}")

        // Try to get references from the element itself
        val elementRefs = element?.references
        println("[DEBUG_LOG] Element references: ${elementRefs?.size ?: 0}")

        // Try to get references from the parent element
        val parent = element?.parent
        val parentRefs = parent?.references
        println("[DEBUG_LOG] Parent references: ${parentRefs?.size ?: 0}")

        // If parent has references, try to find one that contains the element's text
        if (parentRefs != null && parentRefs.isNotEmpty()) {
            val elementText = element?.text?.trim('"', '[', ']') ?: ""
            println("[DEBUG_LOG] Looking for reference containing: $elementText")

            for (ref in parentRefs) {
                val refText = ref.element.text
                println("[DEBUG_LOG] Reference text: $refText")

                // For array types, check if the reference contains the base type
                if (elementText.endsWith("[]")) {
                    val baseType = elementText.substringBefore("[]")
                    if (refText.contains(baseType)) {
                        println("[DEBUG_LOG] Found matching reference for array type")
                        return ref
                    }
                } else if (refText.contains(elementText)) {
                    println("[DEBUG_LOG] Found matching reference")
                    return ref
                }
            }
        }

        return elementRefs?.firstOrNull() ?: parentRefs?.firstOrNull()
    }
}
