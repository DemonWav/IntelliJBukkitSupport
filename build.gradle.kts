/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        maven {
            name = "intellij-plugin-service"
            setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
        }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", properties["kotlinVersion"] as String))
    }
}

val CI = System.getenv("CI") != null

val ideaVersion: String by extra
val javaVersion: String by extra
val kotlinVersion: String by extra
val downloadIdeaSources: String by extra

defaultTasks("build")

apply {
    plugin("kotlin")
}

plugins {
    groovy
    idea
    id("org.jetbrains.intellij") version "0.2.5"
    id("net.minecrell.licenser") version "0.3"
}

val clean: Delete by tasks
val processResources: AbstractCopyTask by tasks
val runIde: JavaExec by tasks
val compileKotlin by tasks

configurations {
    "mixin" {
        isTransitive = false
    }
}

repositories {
    mavenCentral()
    maven {
        name = "sponge"
        setUrl("https://repo.spongepowered.org/maven")
    }
}

dependencies {
    compile(kotlinModule("stdlib-jre8")) {
        // JetBrains annotations are already bundled with IntelliJ IDEA
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // Add tools.jar for the JDI API
    compile(files(Jvm.current().toolsJar))

    // Add an additional dependency on kotlin-runtime. It is essentially useless
    // (since kotlin-runtime is a transitive dependency of kotlin-stdlib-jre8)
    // but without kotlin-stdlib or kotlin-runtime on the classpath,
    // gradle-intellij-plugin will add IntelliJ IDEA's Kotlin version to the
    // dependencies which conflicts with our newer version.
    compile(kotlinModule("runtime")) {
        isTransitive = false
    }

    "mixin"("org.spongepowered:mixin:0.6.8-SNAPSHOT:thin")
}

intellij {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins("maven", "gradle", "Groovy",
        // needed dependencies for unit tests
        "properties", "junit")

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = false

    downloadSources = !CI && downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

java {
    setSourceCompatibility(javaVersion)
    setTargetCompatibility(javaVersion)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion
}

processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
}

tasks.withType<Test> {
    if (CI) systemProperty("slowCI", "true")

    doFirst {
        systemProperty("mixinUrl", configurations["mixin"].files.single().absolutePath)
    }
}

idea {
    module.apply {
        generatedSourceDirs.add(file("gen"))
        excludeDirs.add(file(intellij().sandboxDirectory))
    }
}

// License header formatting
license {
    header = file("copyright.txt")
    include("**/*.java", "**/*.kt", "**/*.groovy", "**/*.gradle", "**/*.xml", "**/*.properties", "**/*.html")
    exclude("com/demonwav/mcdev/platform/mcp/at/gen/**")
}

// Credit for this intellij-rust
// https://github.com/intellij-rust/intellij-rust/blob/d6b82e6aa2f64b877a95afdd86ec7b84394678c3/build.gradle#L131-L181
val generateAtLexer = task<JavaExec>("generateAtLexer") {
    val src = "src/main/grammars/AtLexer.flex"
    val skeleton = "libs/idea-flex.skeleton"
    val dst = "gen/com/demonwav/mcdev/platform/mcp/at/gen/"
    val output = "$dst/AtLexer.java"

    doFirst {
        delete(output)
    }

    classpath = files("libs/jflex-1.7.0-SNAPSHOT.jar")
    main = "jflex.Main"

    args(
        "--skel", skeleton,
        "-d", dst,
        src
    )

    inputs.files(src, skeleton)
    outputs.file(output)
}

/*
 * This helps us get around the command length issues on Windows by placing the classpath in the manifest of a single
 * jar, rather than printing them out in one long line
 */
val pathingJar = task<Jar>("pathingJar") {
    dependsOn(configurations.compile)
    appendix = "pathing"

    doFirst {
        manifest.apply {
            attributes["Class-Path"] = configurations.compile.files.map { file ->
                file.toURI().toString().replaceFirst("file:/+".toRegex(), "/")
            }.joinToString(" ")
        }
    }
}

val generateAtPsiAndParser = task<JavaExec>("generateAtPsiAndParser") {
    val src = "src/main/grammars/AtParser.bnf".replace("/", File.separator)
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/platform/mcp/at/gen".replace("/", File.separator)
    val psiDir = "$dst/psi/".replace("/", File.separator)
    val parserDir = "$dst/parser/".replace("/", File.separator)

    doFirst {
        delete(psiDir, parserDir)
    }

    main = "org.intellij.grammar.Main"

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(mapOf(
        "psi" to psiDir,
        "parser" to parserDir
    ))

    classpath(pathingJar, file("libs/grammar-kit-1.5.1.jar"))
}

val generate = task("generate") {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    dependsOn(generateAtLexer, generateAtPsiAndParser)
    outputs.dir("gen")
}

java().sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.srcDir(generate)

// Remove gen directory on clean
clean.delete(generate)

// Workaround for KT-16764
compileKotlin.inputs.dir(generate)

// Use custom JRE for running IntelliJ IDEA when configured
findProperty("intellijJre")?.let(runIde::setExecutable)

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
fun KotlinDependencyHandler.kotlinModule(module: String) = kotlinModule(module, kotlinVersion) as String
