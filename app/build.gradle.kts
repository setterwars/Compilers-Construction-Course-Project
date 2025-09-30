plugins {
    application
    kotlin("jvm") version "2.2.10"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.github.setterwars.compilercourse.MainKt"
}