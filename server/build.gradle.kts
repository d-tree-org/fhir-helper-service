import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("io.ktor.plugin") version libs.versions.ktor
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.10"
}


group = "org.dtree.fhir"
version = "1.0-SNAPSHOT"

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":core")))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    implementation(libs.ktor.server.webjars)
    implementation(libs.jquery)
    implementation(libs.ktor.swagger.ui)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.call.logging)

    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)

    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.dotenv.kotlin)

    implementation(libs.hapi.fhir.structures.r4)
    implementation(libs.hapi.fhir.validation)
    implementation(libs.hapi.fhir.caching.guava)
    implementation(libs.hapi.fhir.client.okhttp)

    implementation(libs.spullara.mustache)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}