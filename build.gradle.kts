plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    val springVersion = "6.1.5"
    implementation("org.springframework:spring-core:${springVersion}")
    implementation("org.springframework:spring-beans:${springVersion}")
    implementation("org.springframework:spring-context-support:${springVersion}")
    implementation("org.springframework:spring-aop:${springVersion}")

    implementation("org.aspectj:aspectjrt:1.9.22")
    implementation("org.aspectj:aspectjweaver:1.9.22")
}

tasks.test {
    useJUnitPlatform()
}
