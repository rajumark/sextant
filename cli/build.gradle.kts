plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.clikt)
    runtimeOnly(libs.slf4j.nop)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    applicationName = "sextant"
    mainClass.set("io.github.rajumark.sextant.cli.MainKt")
    // sqlite-jdbc loads a native library; JDK 22+ warns unless native access is enabled (flag exists since JDK 17).
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.shadowJar {
    archiveBaseName.set("sextant")
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest.attributes("Enable-Native-Access" to "ALL-UNNAMED")
    // Keep SQLite natives for 64-bit macOS, Linux (glibc + musl) and Windows only (jar 23 MB → 16 MB).
    listOf(
        "FreeBSD",
        "Linux-Android",
        "Linux/arm",
        "Linux/armv6",
        "Linux/armv7",
        "Linux/ppc64",
        "Linux/riscv64",
        "Linux/x86",
        "Linux-Musl/x86",
        "Windows/x86",
        "Windows/armv7",
    ).forEach { exclude("org/sqlite/native/$it/**") }
}

// End-to-end tests run the real fat jar as a subprocess.
tasks.test {
    dependsOn(tasks.shadowJar)
    val jar = tasks.shadowJar.flatMap { it.archiveFile }
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Dsextant.jar=${jar.get().asFile.absolutePath}") })
}

// Release archive: sextant-<version>.zip with bin/sextant, bin/sextant.bat and lib/sextant-<version>.jar.
distributions {
    named("shadow") {
        distributionBaseName.set("sextant")
    }
}

// Only the shadow (fat jar) distribution is shipped; the plain one would clash on the same file name.
tasks.named("distZip") { enabled = false }
tasks.named("distTar") { enabled = false }
tasks.named("shadowDistTar") { enabled = false }
