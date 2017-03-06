/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.facet.MinecraftFacetConfiguration
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.JavaTestUtil
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

abstract class BaseMinecraftTest(protected vararg val platformTypes: PlatformType) : ProjectBuilderTest() {

    protected open fun preConfigureModule(module: Module, model: ModifiableRootModel) {}
    protected open fun postConfigureModule(module: Module, model: ModifiableRootModel) {}

    protected open val resourcePath = "src/test/resources"
    protected open val packagePath = "com/demonwav/mcdev"
    protected open val dataPath = ""

    override fun getTestDataPath() = "$resourcePath/$packagePath/$dataPath"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)

                preConfigureModule(module, model)

                val facetManager = FacetManager.getInstance(module)
                val configuration = MinecraftFacetConfiguration()
                configuration.state.autoDetectTypes.addAll(platformTypes)

                val facet = facetManager.createFacet(MinecraftFacet.facetType, "Minecraft", configuration, null)
                runWriteTask {
                    val modifiableModel = facetManager.createModifiableModel()
                    modifiableModel.addFacet(facet)
                    modifiableModel.commit()
                }

                postConfigureModule(module, model)
            }

            // TODO: Figure out how to package Mock JDK to speed up builds
            override fun getSdk() = JavaTestUtil.getTestJdk()
        }
    }
}
