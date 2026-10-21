// :ml turns the engine's state into the numbers a learned policy consumes:
// the observation of the seat that is about to move and the fixed-size vector
// it is read as. It depends on the engine and on nothing else: no
// machine-learning runtime is on the classpath yet, that arrives with the
// task that runs the model.
plugins {
    `java-library`
}

dependencies {
    // The types of the rules cross the boundary of the observation's
    // signature, so the engine is an api dependency and not an
    // implementation one.
    api(project(":engine"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
