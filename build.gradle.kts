plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

group = "com.vivart"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.12.0")
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:3.3.3")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

application {
    mainClass.set("com.vivart.MainKt")
}

tasks.jar {
    manifest { attributes["Main-Class"] = "com.vivart.MainKt" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
