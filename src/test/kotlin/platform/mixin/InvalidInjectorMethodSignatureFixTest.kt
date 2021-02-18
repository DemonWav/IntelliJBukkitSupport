/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.testInspectionFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.InvalidInjectorMethodSignatureInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Invalid Injector Method Signature Inspection Test")
class InvalidInjectorMethodSignatureFixTest : BaseMixinTest() {

    private fun doTest(testName: String) {
        buildProject {
            dir("test") {
                java(
                    "MixedInSimple.java",
                    """
                    package test;
                    
                    import java.lang.String;

                    class MixedInSimple {
                        public void simpleMethod(String string, int i) {
                            int i = Integer.parseInt("FF", 16);
                        }
                    }
                    """,
                    configure = false
                )
                java(
                    "MixedInOuter.java",
                    """
                    package test;
                    
                    import java.lang.String;

                    class MixedInOuter {
                        public class MixedInInner {
                            public MixedInInner() {
                            }
                            
                            public MixedInInner(String string) {
                            }
                        }
                    }
                    """,
                    configure = false
                )
            }
        }

        fixture.enableInspections(InvalidInjectorMethodSignatureInspection::class)
        testInspectionFix(fixture, "invalidInjectorMethodSignature/$testName", "Fix method parameters")
    }

    @Test
    @DisplayName("Simple case")
    fun simpleCase() = doTest("simpleCase")

    @Test
    @DisplayName("With captured locals")
    fun withCapturedLocals() = doTest("withCapturedLocals")

    @Test
    @DisplayName("Simple inner ctor")
    fun simpleInnerCtor() = doTest("simpleInnerCtor")

    @Test
    @DisplayName("Inner ctor with locals")
    fun innerCtorWithLocals() = doTest("innerCtorWithLocals")

    @Test
    @DisplayName("Inject without CallbackInfo")
    fun injectWithoutCI() = doTest("injectWithoutCI")

    @Test
    @DisplayName("ModifyArgs")
    fun modifyArgs() = doTest("modifyArgs")
}
