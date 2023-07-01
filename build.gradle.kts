import java.io.IOException
import java.util.concurrent.TimeoutException

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.1.2"
}

fun String.runCommand(): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(projectDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply {
        if (!waitFor(10, TimeUnit.SECONDS)) {
            throw TimeoutException("Failed to execute command: '" + this@runCommand + "'")
        }
    }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }

val gitHash = "git rev-parse --verify HEAD".runCommand()
val clean = "git status --porcelain".runCommand().isEmpty()
val lastTag = "git describe --tags --abbrev=0".runCommand()
val lastVersion = if (lastTag.isEmpty()) "dev" else lastTag.substring(1) // remove the leading 'v'
val commits = "git rev-list --count $lastTag..HEAD".runCommand()
println("Git hash: $gitHash" + if (clean) "" else " (dirty)")

group = "de.bluecolored.bluenbt"
version = lastVersion +
        (if (commits == "0") "" else "-$commits") +
        (if (clean) "" else "-dirty")

println("Version: $version")

val javaTarget = 11
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly ("org.jetbrains:annotations:24.0.1")
    compileOnly ("org.projectlombok:lombok:1.18.28")

    api ("com.google.code.gson:gson:2.8.0")

    annotationProcessor ("org.projectlombok:lombok:1.18.28")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testCompileOnly ("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.28")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

spotless {
    java {
        target ("src/*/java/**/*.java")

        licenseHeaderFile("LICENSE_HEADER")
        indentWithSpaces()
        trimTrailingWhitespace()
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8"
    }
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.javadoc {
    options {
        (this as? StandardJavadocDocletOptions)?.apply {
            links(
                "https://docs.oracle.com/javase/8/docs/api/"
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}
