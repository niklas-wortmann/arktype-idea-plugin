package com.github.niklaswortmann.arktypeideaplugin.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

/**
 * Parser definition for ArkType language.
 * This is a minimal implementation that provides the necessary components for parsing ArkType code.
 * Since ArkType is primarily used for language injection, this implementation is kept simple.
 */
class ArkTypeParserDefinition : ParserDefinition {
    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create() // No comments defined for now
        val FILE = IFileElementType(ArkTypeLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?): Lexer = ArkTypeLexer()

    override fun createParser(project: Project?): PsiParser = object : PsiParser {
        override fun parse(root: IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
            val rootMarker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            rootMarker.done(root)
            return builder.treeBuilt
        }
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement {
        return LeafPsiElement(node.elementType, node.text)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return ArkTypeFile(viewProvider)
    }

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): SpaceRequirements {
        return SpaceRequirements.MAY
    }
}

/**
 * Simple PsiFile implementation for ArkType language.
 */
class ArkTypeFile(viewProvider: FileViewProvider) : PsiFileImpl(viewProvider) {
    init {
        init(ArkTypeParserDefinition.FILE, ArkTypeParserDefinition.FILE)
    }

    override fun getFileType() = ArkTypeFileType.INSTANCE
    override fun toString() = "ArkType File"

    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitFile(this)
    }
}
