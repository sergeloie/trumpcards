// :ml turns the engine's state into the numbers a learned policy consumes:
// the observation of the seat that is about to move, the fixed-size vector it
// is read as, and the ONNX runtime that runs the model over that vector.
plugins {
    `java-library`
}

dependencies {
    // The types of the rules cross the boundary of the observation's
    // signature, so the engine is an api dependency and not an
    // implementation one.
    api(project(":core"))

    // The runtime is an implementation detail: no type of it appears in the
    // signature of the policy.
    implementation(libs.onnxruntime)

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
