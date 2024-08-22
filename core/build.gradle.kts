
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    `java-library`
    kotlin("plugin.serialization") version libs.versions.kotlin
}

group = "org.dtree.fhir"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.json.path)
    implementation(libs.snake.yaml)
    implementation(libs.gson)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.jaxb)
    implementation(libs.jackson.jsr310)
    implementation(libs.kaml)
    implementation(libs.hamcrest.core)
    implementation(libs.slf4j.simple)

    implementation(libs.hapi.fhir.structures.r4)
    implementation(libs.hapi.fhir.validation)
    implementation(libs.hapi.fhir.caching.guava)
    implementation(libs.hapi.fhir.client.okhttp)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlin.logging.jvm)

    implementation(libs.dotenv.kotlin)
    implementation(libs.json.tools.patch)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}


tasks.test {
    useJUnitPlatform()
}