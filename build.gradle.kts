buildscript {
    val kotlinVersion: String by project

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
    }
}

group = "org.ktapi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

plugins {
    `java-library`
}

apply(plugin = "kotlin")

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("org.yaml:snakeyaml:1.30")
    compileOnly("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    compileOnly("io.mockk:mockk:1.12.4")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation("io.mockk:mockk:1.12.4")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "14"
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}
