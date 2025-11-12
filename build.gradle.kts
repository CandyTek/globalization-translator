import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.renderer.html.HtmlRenderer
import org.jetbrains.changelog.date
import org.commonmark.parser.Parser as MarkdownParser
import java.io.Reader

plugins {
	id("org.jetbrains.intellij.platform") version "2.10.2"
	id("org.jetbrains.kotlin.jvm") version "2.2.21"

    java
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")
    id("org.jetbrains.changelog") version "1.3.1"
}

group = "com.wilinz.globalization"
version = "1.1.0"

repositories {
    maven("https://jitpack.io")
    mavenCentral()
	google()
	intellijPlatform {
        defaultRepositories()
    }
}

configurations.all { exclude("xml-apis", "xml-apis") }

buildscript {
    repositories {
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
    intellijPlatform {
		intellijIdeaCommunity("2025.2.1")
		bundledPlugin("com.intellij.java")
		bundledPlugin("com.intellij.gradle")
		bundledPlugin("org.jetbrains.kotlin")
		pluginVerifier()
    }

    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation(files("libs/java-properties-1.0.7.jar"))
    implementation(files("libs/dom4j-version-2.1.1.jar"))
    testImplementation(kotlin("test"))
}

kotlin {
	jvmToolchain(21)
}

// See https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
		buildSearchableOptions = false
    pluginConfiguration {
        name = "GlobalizationTranslator"

        ideaVersion {
            sinceBuild = "250"
            untilBuild = provider { null }
        }

        description = getDescription("description.md")
        changeNotes = provider { changelog.getLatest().toHTML() }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        }
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
