// Application code shared with plug-ins and their builds. No app dependencies;
// strange-eons-app depends on this module.

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

dependencies {
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
