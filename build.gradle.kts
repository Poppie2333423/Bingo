plugins {
    `java`
}

val paperVersion = "1.21.1-R0.1-SNAPSHOT"

group = "de.example"
version = "1.0.0"

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

