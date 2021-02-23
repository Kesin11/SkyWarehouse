import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application

    kotlin("plugin.serialization") version "1.4.10"

    // For create fatjar with ":shadowJar"
    id("com.github.johnrengelman.shadow") version "6.1.0"

    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

group = "com.kesin11"
version = "0.1.0"

application {
    mainClassName = "MainKt"
}

// ---- Dependencies

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("io.mockk:mockk:1.10.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.1")

    val ktorVersion = "1.4.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("com.google.cloud:google-cloud-storage:1.113.6")
}

dependencyLocking {
    lockAllConfigurations()
}

// ---- Kotlin

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "1.4"
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

distributions {
    create("archive") {
        // Set archive name to skw-$version.(tar|zip)
        distributionBaseName.set("skw")
        // Copy lib/ and bin/ directory that made by shadow
        contents {
            from(tasks["installShadowDist"].outputs)
        }
    }
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
