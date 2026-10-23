// Plug-in API surface. Pure-Java interfaces and value types under
// org.strangeeons.plugins.*; no app dependencies. Plug-ins compile against
// this module; the strange-eons-app module implements its types.

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        name = "Strange Eons Plug-in API"
        description = "Interfaces and value types that Strange Eons plug-ins compile against."
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
