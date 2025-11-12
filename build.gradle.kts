import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.renderer.html.HtmlRenderer
import org.jetbrains.changelog.date
import org.commonmark.parser.Parser as MarkdownParser
import java.io.Reader

plugins {
    // id("org.jetbrains.intellij") version "1.17.3"
    id("org.jetbrains.intellij") version "1.17.4"
	// id("org.jetbrains.intellij.platform") version "2.10.4"
	// id("org.jetbrains.intellij.platform") version "2.0.0"

    java
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")
    id("org.jetbrains.changelog") version "1.3.1"
}

group = "com.wilinz.globalization"
version = "1.1.0"

repositories {
		maven { url = uri("https://maven.aliyun.com/repository/google") }
		maven { url = uri("https://maven.aliyun.com/repository/central") }
		maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
    // mavenCentral()
    google()
	}

configurations.all { exclude("xml-apis", "xml-apis") }

buildscript {
    repositories {
		maven { url = uri("https://maven.aliyun.com/repository/google") }
		maven { url = uri("https://maven.aliyun.com/repository/central") }
		maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		maven { url = uri("https://jitpack.io") }
		google()
		mavenLocal()
	}
    dependencies {
        classpath("org.commonmark:commonmark:0.18.1")
        // https://mavenlibs.com/maven/dependency/com.atlassian.commonmark/commonmark-ext-gfm-strikethrough
        classpath("com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.17.0")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.github.wilinz:java-properties:1.0.7")
    implementation("com.github.dom4j:dom4j:version-2.1.1")
    testImplementation(kotlin("test"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    // version.set("2021.3") // not support
    // version.set("2022.1.1")
    // version.set("2022.2") // plugin its ok
    // version.set("2022.3") // plugin its ok
    // version.set("2023.1.1") // plugin its ok
    // version.set("2023.3.2") // plugin its ok
    version.set("2024.2.4") // plugin its ok
    // version.set("2025.2.1") // too new, runIde has issues
    // version.set("2025.1") // too new, unstable
    // version.set("2025.1.2") // 
    type.set("IC")
    plugins.set(
        listOf(
            // "org.jetbrains.compose.intellij.platform:0.1.0",
            // "org.jetbrains.kotlin"
        )
    )
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        // sourceCompatibility = "17"
        // targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }
    patchPluginXml {
        sinceBuild.set("202")  // IntelliJ 2024.2+
        untilBuild.set("250")     // No upper limit
        pluginDescription.set(getDescription("description.md"))
        changeNotes.set(provider { changelog.getLatest().toHTML() })
    }
    // Skip buildSearchableOptions for compatibility with newer IDE versions
    buildSearchableOptions {
        enabled = false
    }
}

fun getDescription(relative: String): String {
    return projectDir.resolve(relative).reader().use {
        markdownToHtml(it)
    }
}

fun markdownToHtml(reader: Reader): String {
    val parser = MarkdownParser.builder()
        .extensions(listOf(StrikethroughExtension.create()))
        .build()
    val document = parser.parseReader(reader)
    val renderer = HtmlRenderer.builder().build()
    return renderer.render(document)
}

changelog {
    version.set(project.version.toString())
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}
