// Application code shared with plug-ins and their builds. No app dependencies;
// strange-eons-app depends on this module. Targets Java 17 because the testkit,
// which Gradle plug-ins load in the build JVM, depends on it.

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        name = "Strange Eons Core"
        description = "Strange Eons code shared by the application and plug-ins."
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

dependencies {
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
