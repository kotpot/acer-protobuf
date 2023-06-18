import com.google.protobuf.gradle.id

plugins {
    java
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.3"
}

val protocolVersion = "3.22.5"

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin-lite:$protocolVersion")
    implementation("com.google.protobuf:protobuf-javalite:$protocolVersion")
}

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.3")
    }
}

sourceSets {
    main {
        proto {
            srcDirs("proto/net")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protocolVersion"
    }

    plugins {
    }

    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.builtins {
                getByName("java") { // Java Scope
                    option("lite")
                }
                id("kotlin") { // Kotlin Scope
                    option("lite")
                }
            }
            task.plugins {
            }
        }
    }
}
