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

import com.demonwav.mcdev.i18n.Scope
import com.demonwav.mcdev.i18n.findProperties
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project

class I18nGotoSymbolContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val properties = if (includeNonProjectItems) project.findProperties() else project.findProperties(Scope.PROJECT)
        val names = properties.filter { it.key.isNotEmpty() }.map { it.key }
        return names.toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val properties = if (includeNonProjectItems) project.findProperties(key = name) else project.findProperties(Scope.PROJECT, name)
        return properties.map { it as NavigationItem }.toTypedArray()
    }
}