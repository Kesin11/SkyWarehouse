import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    application

    // For create fatjar with ":shadowJar"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"

    id("org.beryx.runtime") version "1.12.7"
}

group = "com.kesin11"
version = "0.2.0"

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
    minimize()
}

runtime {
    // jpackage default jlink optimization options
    options.set(listOf("--strip-native-commands", "--strip-debug", "--no-header-files", "--no-man-pages"))
    // jpackage default jlink --add-modules options
    modules.set(listOf(
        "java.base",
        "java.compiler",
        "java.datatransfer",
        "java.desktop",
        "java.instrument",
        "java.logging",
        "java.management",
        "java.management.rmi",
        "java.naming",
        "java.net.http",
        "java.prefs",
        "java.rmi",
        "java.scripting",
        "java.security.jgss",
        "java.security.sasl",
        "java.smartcardio",
        "java.sql",
        "java.sql.rowset",
        "java.transaction.xa",
        "java.xml",
        "java.xml.crypto",
        "jdk.accessibility",
        "jdk.attach",
        "jdk.charsets",
        "jdk.compiler",
        "jdk.crypto.cryptoki",
        "jdk.crypto.ec",
        "jdk.dynalink",
        "jdk.editpad",
        "jdk.httpserver",
        "jdk.incubator.foreign",
        "jdk.incubator.vector",
        "jdk.internal.ed",
        "jdk.internal.jvmstat",
        "jdk.internal.le",
        "jdk.internal.opt",
        "jdk.jartool",
        "jdk.javadoc",
        "jdk.jconsole",
        "jdk.jdeps",
        "jdk.jdi",
        "jdk.jdwp.agent",
        "jdk.jfr",
        "jdk.jlink",
        "jdk.jpackage",
        "jdk.jshell",
        "jdk.jsobject",
        "jdk.jstatd",
        "jdk.localedata",
        "jdk.management",
        "jdk.management.agent",
        "jdk.management.jfr",
        "jdk.naming.dns",
        "jdk.naming.rmi",
        "jdk.net",
        "jdk.nio.mapmode",
        "jdk.sctp",
        "jdk.security.auth",
        "jdk.security.jgss",
        "jdk.unsupported",
        "jdk.unsupported.desktop",
        "jdk.xml.dom",
        "jdk.zipfs",
    ))
    jpackage {
        installerType = "deb"
        installerOptions = listOf("--install-dir", "/usr/local")
    }
}
// ---- Aggregate artifacts and copy to "distributions" directory
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
val archiveShadowJarTask = tasks.register<Copy>("archiveShadowJar") {
    group = ARCHIVE_GROUP
    description = "Copy and rename shadow jar to skw.jar"
    outputs.upToDateWhen { false }

    from(tasks["shadowJar"].outputs)
    include("*-all.jar")
    rename(".+-all.jar", "${rootProject.name}.jar")
    into(layout.buildDirectory.dir("distributions"))
}
val archiveJpackageTask = tasks.register<Copy>("archiveJpackageJar") {
    group = ARCHIVE_GROUP
    description = "Copy jpackage output into distributions"
    outputs.upToDateWhen { false }

    from(tasks["jpackage"].outputs)
    include("*.deb")
    into(layout.buildDirectory.dir("distributions"))
    rename("skw_.+_amd64\\.deb", "skw_amd64.deb")
}
val assembleArchiveTask = tasks.register("assembleArchive") {
    group = ARCHIVE_GROUP
    description = "Assembles the archive"
    dependsOn(archiveTarTask, archiveZipTask, archiveShadowJarTask, archiveJpackageTask)
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
