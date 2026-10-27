import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

// Shared settings for every Kotlin module: JVM 17 bytecode, JUnit 5, ktlint.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xjdk-release=17")
                allWarningsAsErrors.set(true)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
