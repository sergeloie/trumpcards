// :core is the rules module. It depends on nothing but the JDK: plain records
// instead of Lombok, and nothing that would reach a runtime classpath.
plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.junit.jupiter)
    // The tests drive the rules with the rule bot of the :bots module. The two
    // modules depend on each other in the test scope only: the bot needs the
    // rules' types, the tests need the bot. The task graph stays acyclic,
    // because :core:compileTestJava needs :bots:jar, and :bots:jar needs
    // :core:jar.
    testImplementation(project(":bots"))
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
