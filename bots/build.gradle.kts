// :bots holds the fixed policies. It depends on the rules and on nothing
// else: a policy is a function from a state and the legal moves to an index.
plugins {
    `java-library`
}

dependencies {
    // The types of the rules cross the boundary of the policy's signature,
    // so the engine is an api dependency and not an implementation one.
    api(project(":core"))

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
