package com.github.niklaswortmann.arktypeideaplugin

import com.github.niklaswortmann.arktypeideaplugin.language.ArkTypeDocumentationProvider
import com.github.niklaswortmann.arktypeideaplugin.language.ArkTypeFileType
import com.github.niklaswortmann.arktypeideaplugin.language.ArkTypeSyntaxHighlighter
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for the ArkTypeDocumentationProvider.
 */
class ArkTypeDocumentationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    /**
     * Test that documentation is provided for ArkType keywords.
     */
    fun testKeywordDocumentation() {
        // Create a test file with ArkType content containing keywords
        val testContent = """
            const Keywords = type({
                string: "string",
                number: "number",
                boolean: "boolean"
            })
        """.trimIndent()

        // Configure the editor with the test content
        myFixture.configureByText(ArkTypeFileType.INSTANCE, testContent)

        // Get the documentation provider
        val docProvider = ArkTypeDocumentationProvider()

        // Test the documentation provider for each keyword
        for (keyword in ArkTypeSyntaxHighlighter.KEYWORDS) {
            val doc = docProvider.generateKeywordDoc(keyword)

            // Verify that documentation is provided
            assertNotNull("Documentation should be provided for keyword: $keyword", doc)
            assertTrue("Documentation should contain the keyword: $keyword", doc.contains(keyword))
            assertTrue("Documentation should be properly formatted", 
                doc.contains(DocumentationMarkup.DEFINITION_START) && 
                doc.contains(DocumentationMarkup.CONTENT_END))
        }
    }

    /**
     * Test that documentation is provided for ArkType subtypes.
     */
    fun testSubtypeDocumentation() {
        // Create a test file with ArkType content containing subtypes
        val testContent = """
            const Subtypes = type({
                dateString: "string.date",
                integerNumber: "number.integer",
                positiveNumber: "number.positive"
            })
        """.trimIndent()

        // Configure the editor with the test content
        myFixture.configureByText(ArkTypeFileType.INSTANCE, testContent)

        // Get the documentation provider
        val docProvider = ArkTypeDocumentationProvider()

        // Test the documentation provider for each subtype
        for (subtype in ArkTypeSyntaxHighlighter.SUBTYPES) {
            val doc = docProvider.generateSubtypeDoc(subtype)

            // Verify that documentation is provided
            assertNotNull("Documentation should be provided for subtype: $subtype", doc)
            assertTrue("Documentation should contain the subtype: $subtype", doc.contains(subtype))
            assertTrue("Documentation should be properly formatted", 
                doc.contains(DocumentationMarkup.DEFINITION_START) && 
                doc.contains(DocumentationMarkup.CONTENT_END))
        }
    }
}
