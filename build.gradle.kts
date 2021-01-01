import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application

    kotlin("plugin.serialization") version "1.4.10"

    // For create fatjar with ":shadowJar"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.kesin11"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven ( url = "https://kotlin.bintray.com/kotlinx" )
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("io.mockk:mockk:1.10.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.1")

    val ktorVersion = "1.4.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("com.google.cloud:google-cloud-storage:1.113.6")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    archiveFileName.set("skw.jar")
}

tasks.withType<Test> {
    this.testLogging {
        this.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        this.showStackTraces = false
    }
}

application {
    mainClassName = "MainKt"
}
