package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.intellij.codeInsight.actions.SimpleCodeInsightAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class MixinCodeInsightAction : SimpleCodeInsightAction() {

    override fun startInWriteAction() = false

    // Display action in Mixin classes only
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language != JavaLanguage.INSTANCE) {
            return false
        }

        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        return MixinUtils.getContainingMixinClass(element) != null
    }

}
