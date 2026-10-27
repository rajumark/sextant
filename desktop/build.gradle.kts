import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.swing)
    // Builds the full Material 3 color scheme from one seed color (Material Color Utilities).
    implementation(libs.material.kolor)
    runtimeOnly(libs.slf4j.nop)

    testImplementation(kotlin("test"))
    testImplementation(libs.compose.ui.test)
}

compose.desktop {
    application {
        mainClass = "io.github.rajumark.sextant.desktop.MainKt"
        // sqlite-jdbc (the code index) loads a native library.
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sextant"
            // Installers need MAJOR > 0 on macOS, so the app is versioned separately from the 0.x CLI.
            packageVersion = "1.0.0"
            modules("java.sql")
        }
    }
}

tasks.test {
    // Screenshot tests render off-screen; the UI tests need headless-safe AWT.
    systemProperty("java.awt.headless", "true")
    // Real-APK corpus (scripts/fetch-apks.sh); the repository test skips when it is missing.
    systemProperty("sextant.corpus", rootProject.file("fixtures/external").path)
    systemProperty(
        "sextant.shots",
        System.getenv("SEXTANT_SHOTS_DIR") ?: layout.buildDirectory
            .dir("screenshots")
            .get()
            .asFile.path,
    )
}
