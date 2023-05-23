plugins {
    id("java")
}

group = "com.chaottic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("commons-cli:commons-cli:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}