plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.google.cloud.tools.jib") version "3.4.0"
}

group = "ru"
version = "0.0.1-SNAPSHOT"
description = "ksolow-tools"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

jib {
    from {
        image = "eclipse-temurin:17-jre"
    }
    to {
        image = System.getenv("DOCKER_HUB_REGISTRY")
        tags = setOf("latest", version.toString())
        auth {
            username = System.getenv("DOCKER_HUB_USERNAME")
            password = System.getenv("DOCKER_HUB_PASSWORD")
        }
    }
    container {
        ports = listOf("8080")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context-support")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("org.jsoup:jsoup:1.17.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
