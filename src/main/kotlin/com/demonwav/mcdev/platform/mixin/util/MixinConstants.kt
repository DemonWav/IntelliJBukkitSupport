/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

object MixinConstants {
    const val PACKAGE = "org.spongepowered.asm.mixin."

    object Classes {
        const val INJECTION_POINT = "org.spongepowered.asm.mixin.injection.InjectionPoint"
        const val CALLBACK_INFO = "org.spongepowered.asm.mixin.injection.callback.CallbackInfo"
        const val CALLBACK_INFO_RETURNABLE = "org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"
    }

    object Annotations {
        const val AT = "org.spongepowered.asm.mixin.injection.At"
        const val DEBUG = "org.spongepowered.asm.mixin.Debug"
        const val FINAL = "org.spongepowered.asm.mixin.Final"
        const val IMPLEMENTS = "org.spongepowered.asm.mixin.Implements"
        const val INTERFACE = "org.spongepowered.asm.mixin.Interface"
        const val INTRINSIC = "org.spongepowered.asm.mixin.Intrinsic"
        const val MIXIN = "org.spongepowered.asm.mixin.Mixin"
        const val MUTABLE = "org.spongepowered.asm.mixin.Mutable"
        const val OVERWRITE = "org.spongepowered.asm.mixin.Overwrite"
        const val SHADOW = "org.spongepowered.asm.mixin.Shadow"
        const val SOFT_OVERRIDE = "org.spongepowered.asm.mixin.SoftOverride"
        const val UNIQUE = "org.spongepowered.asm.mixin.Unique"
        const val INJECT = "org.spongepowered.asm.mixin.injection.Inject"
        const val MODIFY_ARG = "org.spongepowered.asm.mixin.injection.ModifyArg"
        const val MODIFY_CONSTANT = "org.spongepowered.asm.mixin.injection.ModifyConstant"
        const val MODIFY_VARIABLE = "org.spongepowered.asm.mixin.injection.ModifyVariable"
        const val REDIRECT = "org.spongepowered.asm.mixin.injection.Redirect"
        const val SURROGATE = "org.spongepowered.asm.mixin.injection.Surrogate"
    }

}
