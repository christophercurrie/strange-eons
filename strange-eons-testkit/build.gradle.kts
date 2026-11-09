// Test support for plug-ins: what plug-in builds need but the app does not.
// Targets Java 17 so Gradle plug-ins can load it in the build JVM.

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        name = "Strange Eons Testkit"
        description = "Test support for Strange Eons plug-ins."
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
