/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.findDefaultProperties
import com.demonwav.mcdev.i18n.findProperties
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.demonwav.mcdev.util.toArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.IncorrectOperationException

class I18nReference(element: PsiElement,
                    textRange: TextRange,
                    private val useDefault: Boolean,
                    val key: String,
                    val varKey: String) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project
        val properties = if (useDefault) project.findDefaultProperties(key = key) else project.findProperties(key = key)
        return properties.map(::PsiElementResolveResult).toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return resolveResults.singleOrNull()?.element
    }

    override fun getVariants(): Array<Any?> {
        val project = myElement.project
        val properties = project.findDefaultProperties()
        val stringPattern =
            if (varKey.contains(VARIABLE_MARKER)) {
                varKey.split(VARIABLE_MARKER).map { Regex.escape(it) }.joinToString("(.*?)")
            } else {
                "(" + Regex.escape(varKey) + ".*?)"
            }
        val pattern = Regex(stringPattern)
        return properties
            .filter { it.key.isNotEmpty() }
            .mapNotNull { property -> pattern.matchEntire(property.key)?.let { property to it } }
            .map { (property, match) ->
                LookupElementBuilder
                    .create(if (match.groups.size <= 1) property.key else match.groupValues[1])
                    .withIcon(PlatformAssets.MINECRAFT_ICON)
                    .withTypeText(property.containingFile.name)
                    .withPresentableText(property.key)
            }
            .toArray()
    }

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        val stringPattern =
            if (varKey.contains(VARIABLE_MARKER)) {
                varKey.split(VARIABLE_MARKER).map { Regex.escape(it) }.joinToString("(.*?)")
            } else {
                "(" + Regex.escape(varKey) + ")"
            }
        val pattern = Regex(stringPattern)
        val match = pattern.matchEntire(newElementName)
        return super.handleElementRename(if (match != null && match.groups.size > 1) match.groupValues[1] else newElementName)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is I18nProperty && element.key == key
    }

    companion object {
        const val VARIABLE_MARKER = "\$IDEA_TRANSLATION_VARIABLE"
    }
}