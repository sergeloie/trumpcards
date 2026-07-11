plugins {
    application
}

dependencies {
    implementation(libs.guava)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "ru.anseranser.App"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
