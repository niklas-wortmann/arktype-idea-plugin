package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lang.Language

/**
 * ArkType language definition.
 * This is a virtual language for ArkType that can be injected instead of the full TypeScript language.
 * It supports the keywords available in ArkType as documented in keywords.md.
 */
class ArkTypeLanguage private constructor() : Language("ArkType") {
    companion object {
        @JvmStatic
        val INSTANCE = ArkTypeLanguage()
    }

    override fun isCaseSensitive(): Boolean = true
    override fun getDisplayName(): String = "ArkType"
}