/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("UtilKt")
package com.demonwav.mcdev.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil

// Kotlin functions
// can't inline them due to a bug in the compiler
fun runWriteTask(func: () -> Unit) {
    invokeAndWait { runWriteAction(func) }
}

fun runWriteTaskLater(func: () -> Unit) {
    invokeLater { runWriteAction(func) }
}

fun invokeAndWait(func: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait { func() }
}

fun invokeLater(func: () -> Unit) {
    ApplicationManager.getApplication().invokeLater { func() }
}

/**
 * Returns an untyped array for the specified [Collection].
 */
internal fun Collection<*>.toArray(): Array<Any?> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    return (this as java.util.Collection<*>).toArray()
}

@Suppress("UNCHECKED_CAST")
internal inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
    if (this is List) {
        return this.mapToArray(transform)
    }

    val result = arrayOfNulls<R>(size)
    var i = 0
    for (e in this) {
        result[i++] = transform(e)
    }
    return result as Array<R>
}

internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size, { i -> transform(this[i]) })
}

object Util {
    // Java static methods
    @JvmStatic
    fun runWriteTask(func: Runnable) {
        runWriteTask { func.run() }
    }

    @JvmStatic
    fun runWriteTaskLater(func: Runnable) {
        runWriteTaskLater { func.run() }
    }

    @JvmStatic
    fun invokeAndWait(func: Runnable) {
        invokeAndWait { func.run() }
    }

    @JvmStatic
    fun invokeLater(func: Runnable) {
        invokeLater { func.run() }
    }

    @JvmStatic
    fun defaultNameForSubClassEvents(psiClass: PsiClass): String {
        val isInnerClass = psiClass.parent !is PsiFile

        val name = StringBuilder()
        if (isInnerClass) {
            val containingClass = PsiUtil.getContainingNotInnerClass(psiClass)
            if (containingClass != null) {
                if (containingClass.name != null) {
                    name.append(containingClass.name!!.replace("Event".toRegex(), ""))
                }
            }
        }

        var className = psiClass.name!!
        if (className.startsWith(name.toString())) {
            className = className.substring(name.length)
        }
        name.append(className.replace("Event".toRegex(), ""))

        name.insert(0, "on")
        return name.toString()
    }
}
