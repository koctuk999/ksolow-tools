plugins {
    kotlin("jvm")
    `maven-publish`
}

description = "ksolow-tools-telegram-client"

group = "ru.ksolowtools"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    withSourcesJar()
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.7")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.litote.kmongo:kmongo:4.11.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
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

publishing {
    publications {
        create<MavenPublication>("githubPackages") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "telegram-client"
            version = project.version.toString()
        }
    }

    repositories {
        val githubOwner = githubOwnerOrNull()
        val githubRepository = githubRepositoryOrNull()

        if (githubOwner != null && githubRepository != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/$githubOwner/$githubRepository")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                        .orNull
                    password = providers.gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orElse(providers.environmentVariable("GITHUB_PASSWORD"))
                        .orNull
                }
            }
        }
    }
}

fun githubOwnerOrNull(): String? =
    providers.gradleProperty("githubOwner")
        .orElse(
            providers.environmentVariable("GITHUB_REPOSITORY")
                .map { repository -> repository.substringBefore("/") }
        )
        .orNull

fun githubRepositoryOrNull(): String? =
    providers.gradleProperty("githubRepository")
        .orElse(
            providers.environmentVariable("GITHUB_REPOSITORY")
                .map { repository -> repository.substringAfter("/") }
        )
        .orNull
