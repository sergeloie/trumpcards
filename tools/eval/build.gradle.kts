// :tools:eval measures a learned policy against the rule bot: the learned
// policy takes one seat, the rule bot takes the others, and the tool reports
// how often each seat won over a seed range. It asserts the same invariants
// while it plays, so that a policy that breaks the rules is caught even when
// it is winning.
plugins {
    `java-library`
    application
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":bots"))
    implementation(project(":ml"))

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

application {
    mainClass = "ru.anseranser.eval.Eval"
}
