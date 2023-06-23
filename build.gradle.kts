import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.8.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.5.1")
    implementation(kotlin("stdlib-jdk8"))
}

sourceSets {
    main {
        kotlin {
            srcDirs(
                "generate"
            )
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.5"
    freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    jvmTarget = "1.8"
}