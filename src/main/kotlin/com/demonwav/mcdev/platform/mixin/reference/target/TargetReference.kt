/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.createMethodReference
import com.demonwav.mcdev.platform.mixin.util.findSource
import com.demonwav.mcdev.util.findParent
import com.demonwav.mcdev.util.mapToArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext

internal class MixinTargetReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val at = findParent<PsiAnnotation>(element, false)!!
        val injectionPointTypeValue = at.findAttributeValue("value") as? PsiLiteral ?: return PsiReference.EMPTY_ARRAY
        val type = TargetReferenceInjectionPointType.find(injectionPointTypeValue.value as String)
                ?: return PsiReference.EMPTY_ARRAY
        val reference = type.createReference(element, at) ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(reference)
    }

}

private enum class TargetReferenceInjectionPointType(vararg val types: String) {
    INVOKE("INVOKE", "INVOKE_ASSIGN") {
        override fun createReferenceForMethod(element: PsiLiteral, methodReference: MixinReference): MixinReference {
            return MethodTargetReference(element, methodReference)
        }
    },
    INVOKE_STRING("INVOKE_STRING") {
        override fun createReferenceForMethod(element: PsiLiteral, methodReference: MixinReference): MixinReference {
            return ConstantStringMethodTargetReference(element, methodReference)
        }

    },
    FIELD("FIELD") {
        override fun createReferenceForMethod(element: PsiLiteral, methodReference: MixinReference): MixinReference {
            return FieldTargetReference(element, methodReference)
        }
    },
    NEW("NEW") {
        override fun createReferenceForMethod(element: PsiLiteral, methodReference: MixinReference): MixinReference {
            return ConstructorTargetReference(element, methodReference)
        }
    };

    abstract fun createReferenceForMethod(element: PsiLiteral, methodReference: MixinReference): MixinReference

    fun createReference(element: PsiElement, at: PsiAnnotation): MixinReference? {
        val injectorAnnotation = findParent<PsiAnnotation>(at.parent, false)
        val methodValue = injectorAnnotation!!.findAttributeValue("method")!! as? PsiLiteral ?: return null
        val methodReference = createMethodReference(methodValue) ?: return null
        return createReferenceForMethod(element as PsiLiteral, methodReference)
    }

    companion object {

        private val types: Map<String, TargetReferenceInjectionPointType>

        fun find(type: String): TargetReferenceInjectionPointType? {
            return types[type]
        }

        init {
            val typeMap = HashMap<String, TargetReferenceInjectionPointType>()
            for (refType in values()) {
                for (type in refType.types) {
                    typeMap[type] = refType
                }
            }
            this.types = typeMap
        }
    }

}

internal abstract class TargetReference<T>(element: PsiLiteral, val methodReference: MixinReference)
    : PsiReferenceBase.Poly<PsiLiteral>(element), MixinReference {

    protected val targetMethod
        get() = (methodReference.resolve() as? PsiMethod)?.findSource()

    override fun validate() = if (multiResolve(false).isNotEmpty()) MixinReference.State.VALID else MixinReference.State.UNRESOLVED

    protected abstract fun createFindUsagesVisitor(): CollectVisitor<out PsiElement>
    protected abstract fun createCollectMethodsVisitor(): CollectVisitor<T>

    protected abstract fun createLookup(targetClass: PsiClass, element: T): LookupElementBuilder

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val codeBlock = targetMethod?.body ?: return ResolveResult.EMPTY_ARRAY

        val visitor = createFindUsagesVisitor()
        codeBlock.accept(visitor)

        return visitor.result.mapToArray(::PsiElementResolveResult)
    }

    override fun getVariants(): Array<out Any> {
        // TODO: Right now this will only work for Mixins with a single target class
        val target = this.targetMethod ?: return LookupElementBuilder.EMPTY_ARRAY
        val codeBlock = target.body ?: return LookupElementBuilder.EMPTY_ARRAY

        // Collect all possible targets
        val visitor = createCollectMethodsVisitor()
        codeBlock.accept(visitor)

        val targetClass = target.containingClass!!
        return visitor.result
                .mapToArray { createLookup(targetClass, it) }
    }

}

internal abstract class QualifiedTargetReference<T : PsiMember>(element: PsiLiteral, methodReference: MixinReference)
    : TargetReference<QualifiedMember<T>>(element, methodReference) {

    protected abstract fun createLookup(targetClass: PsiClass, m: T, qualifier: PsiClassType?): LookupElementBuilder

    override fun createLookup(targetClass: PsiClass, element: QualifiedMember<T>): LookupElementBuilder {
        return qualifyLookup(createLookup(targetClass, element.member, element.qualifier), targetClass, element.member)
    }

}

internal abstract class CollectVisitor<T> : JavaRecursiveElementWalkingVisitor() {
    val result = ArrayList<T>()
}

internal fun findQualifierType(reference: PsiQualifiedReference): PsiClassType? {
    val qualifier = reference.qualifier ?: return null
    return when (qualifier) {
        is PsiTypeElement -> qualifier.type as? PsiClassType
        is PsiExpression -> qualifier.type as? PsiClassType
        else -> null
    }
}

internal data class QualifiedMember<T : PsiMember>(val member: T, val qualifier: PsiClassType?) {
    constructor(member: T, reference: PsiQualifiedReference) : this(member, findQualifierType(reference))
}

internal fun qualifyLookup(builder: LookupElementBuilder, targetClass: PsiClass, m: PsiMember): LookupElementBuilder {
    val owner = m.containingClass!!
    return if (targetClass.manager.areElementsEquivalent(targetClass, owner)) {
        builder
    } else {
        // Qualify member with name of owning class
        builder.withPresentableText(owner.name + '.' + m.name)
    }
}
