package com.github.niklaswortmann.arktypeideaplugin

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.niklaswortmann.arktypeideaplugin.language.ArkTypeFileType

//class ArkTypeCompletionTest : BasePlatformTestCase() {
//
//    override fun getTestDataPath(): String = "src/test/testData"
//
//    /**
//     * Test that type aliases defined within a scope are suggested in code completion
//     */
//    fun testScopeTypeAliasCompletion() {
//        // Create a test file with ArkType content
//        val fileContent = """
//            import {scope, type} from "arktype";
//
//            // Define a scope with type aliases
//            const coolScope = scope({
//                // Type aliases that should be available for completion within the scope
//                Id: "string",
//                User: { id: "Id", friends: "Id[]" },
//                UsersById: {
//                    "[Id]": "User | undefined"
//                }
//            })
//        """.trimIndent()
//
//        // Configure the editor with the test content
//        myFixture.configureByText(ArkTypeFileType.INSTANCE, fileContent)
//
//        // Position the cursor after "id: " in the User definition
//        val idPosition = myFixture.file.text.indexOf("id: ") + 4
//        myFixture.editor.caretModel.moveToOffset(idPosition)
//
//        // Invoke completion
//        myFixture.complete(CompletionType.BASIC)
//
//        // Check that "Id" is in the completion list
//        val lookupElements = myFixture.lookupElements ?: emptyArray()
//        val hasIdAlias = lookupElements.any { it.lookupString == "Id" }
//
//        assertTrue("Type alias 'Id' should be suggested in completion", hasIdAlias)
//    }
//
//    fun testScopeTypeAliasUserCompletion() {
//        // Create a test file with ArkType content
//        val fileContent = """
//            import {scope, type} from "arktype";
//
//            // Define a scope with type aliases
//            const coolScope = scope({
//                // Type aliases that should be available for completion within the scope
//                Id: "string",
//                User: { id: "Id", friends: "Id[]" },
//                UsersById: {
//                    "[Id]": ""
//                }
//            })
//        """.trimIndent()
//
//        // Configure the editor with the test content
//        myFixture.configureByText(ArkTypeFileType.INSTANCE, fileContent)
//
//        // Position the cursor after "id: " in the User definition
//        val idPosition = myFixture.file.text.indexOf("\"[Id]\": ") + 9
//        myFixture.editor.caretModel.moveToOffset(idPosition)
//
//        // Invoke completion
//        myFixture.complete(CompletionType.BASIC)
//
//        // Check that "Id" is in the completion list
//        val lookupElements = myFixture.lookupElements ?: emptyArray()
//        val hasUserAlias = lookupElements.any { it.lookupString == "User" }
//
//        assertTrue("Type alias 'User' should be suggested in completion", hasUserAlias)
//    }
//}
