plugins {
    kotlin("jvm") version "1.3.61"
}

group = "ca.artemishub"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.cloud:google-cloud-vision:1.99.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.+")

    implementation("org.nield:kotlin-statistics:1.2.1")
    implementation("com.willowtreeapps:fuzzywuzzy-kotlin-jvm:0.1.1")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.7.3")

    implementation("org.anglur:joglext:1.0.3")
    implementation("net.imagej:ij:1.51s")

    implementation("io.github.microutils:kotlin-logging:1.7.8")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.9.1")
    implementation("org.apache.logging.log4j:log4j-api:2.9.1")
    implementation("org.apache.logging.log4j:log4j-core:2.9.1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}