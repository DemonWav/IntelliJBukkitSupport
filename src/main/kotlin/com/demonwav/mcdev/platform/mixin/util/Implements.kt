/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findMatchingMethods
import com.demonwav.mcdev.util.resolveClass
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.annotations.Contract

@Contract(pure = true)
fun PsiClass.findSoftImplements(): Map<String, PsiClass>? {
    val implements = modifierList?.findAnnotation(IMPLEMENTS) ?: return null
    val interfaces = implements.findDeclaredAttributeValue(null)?.findAnnotations() ?: return null
    if (interfaces.isEmpty()) {
        return null
    }

    val result = HashMap<String, PsiClass>()
    for (iface in interfaces) {
        val prefix = iface.findDeclaredAttributeValue("prefix")?.constantStringValue ?: continue
        val psiClass = iface.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue
        result[prefix] = psiClass
    }

    return result
}

@Contract(pure = true)
fun PsiMethod.isSoftImplementMissingParent(): Boolean {
    return findSoftImplementedMethods(true) { return false }
}

@Contract(pure = true)
inline fun PsiMethod.findSoftImplementedMethods(checkBases: Boolean, func: (PsiMethod) -> Unit): Boolean {
    val containingClass = containingClass ?: return false
    val softImplements = containingClass.findSoftImplements() ?: return false
    if (softImplements.isEmpty()) {
        return false
    }

    val methodName = name
    var foundPrefix = false

    for ((prefix, iface) in softImplements) {
        if (!methodName.startsWith(prefix)) {
            continue
        }

        foundPrefix = true
        iface.findMatchingMethods(this, checkBases, methodName.removePrefix(prefix)) { superMethod ->
            if (!superMethod.hasModifierProperty(PsiModifier.STATIC)) {
                func(superMethod)
            }
        }
    }

    return foundPrefix
}
