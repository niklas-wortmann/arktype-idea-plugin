package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * File type for ArkType language.
 * This is a virtual file type that is not meant to be used directly,
 * but is required for the language to be properly recognized by the IDE.
 */
class ArkTypeFileType private constructor() : LanguageFileType(ArkTypeLanguage.INSTANCE) {
    companion object {
        @JvmStatic
        val INSTANCE = ArkTypeFileType()

        private val ICON = IconLoader.getIcon("/icons/arktype.svg", ArkTypeFileType::class.java)
    }

    override fun getName(): String = "ArkType"
    override fun getDescription(): String = "ArkType language file"
    override fun getDefaultExtension(): String = "arktype"
    override fun getIcon(): Icon = ICON
}
