/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.inspection.implements.DuplicateInterfacePrefixInspection
import org.intellij.lang.annotations.Language

class DuplicateInterfacePrefixInspectionTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                    }
                """)

                java("test/DummyFace2.java", """
                    package test;

                    interface DummyFace2 {

                    }
                """)
            }
        }
    }

    private fun doTest(@Language("JAVA") mixinCode: String) {
        buildProject {
            src {
                java("test/DuplicateInterfacePrefixMixin.java", mixinCode)
            }
        }

        myFixture.enableInspections(DuplicateInterfacePrefixInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }

    fun testGood() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                @Interface(iface = DummyFace2.class, prefix = "b$")
            })
            class DuplicateInterfacePrefixMixin {

            }
        """)
    }

    fun testBad() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                @Interface(iface = DummyFace2.class, prefix = <error descr="Duplicate prefix 'a$'">"a$"</error>)
            })
            class DuplicateInterfacePrefixMixin {

            }
        """)
    }
}
