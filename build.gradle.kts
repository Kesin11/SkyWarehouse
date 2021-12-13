import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    application


    // For create fatjar with ":shadowJar"
    id("com.github.johnrengelman.shadow") version "7.1.0"

    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

group = "com.kesin11"
version = "0.1.2"

application {
    mainClass.set("MainKt")
}

// ---- Dependencies

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("io.mockk:mockk:1.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")

    val ktorVersion = "1.4.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("com.google.cloud:google-cloud-storage:1.113.6")
}

dependencyLocking {
    lockAllConfigurations()
    lockMode.set(LockMode.STRICT)
}

// ---- Kotlin

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "1.6"
}

// ---- Create jar archives

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "MainKt",
            "Implementation-Title" to "SkyWarehouse",
            "Implementation-Version" to project.version.toString()
        )
    }
    archiveBaseName.set("skw")
    archiveClassifier.set("")
    archiveVersion.set("")
    minimize()
}

// ---- Crate tar and zip

val ARCHIVE_GROUP = "Archive"
val archiveTarTask = tasks.register<Tar>("archiveTar") {
    group = ARCHIVE_GROUP
    description = "Bundles jar and executable scripts"

    archiveFileName.set("${project.name}.tar")
    from(tasks["installShadowDist"].outputs)
    into("${project.name}")
}
val archiveZipTask = tasks.register<Zip>("archiveZip") {
    group = ARCHIVE_GROUP
    description = "Bundles jar and executable scripts"

    archiveFileName.set("${project.name}.zip")
    from(tasks["installShadowDist"].outputs)
    into("${project.name}")
}
val assembleArchiveTask = tasks.register("assembleArchive") {
    group = ARCHIVE_GROUP
    description = "Assembles the archive"
    dependsOn(archiveTarTask, archiveZipTask)
}
tasks.assemble {
    dependsOn(assembleArchiveTask)
}

// ---- Test and check

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    this.testLogging {
        this.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        this.showStackTraces = false
    }
}

ktlint {
    outputToConsole.set(true)
    coloredOutput.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/style-violations.kt")
    }
}
