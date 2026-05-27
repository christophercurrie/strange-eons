// Plug-in API surface. Pure-Java interfaces and value types under
// org.strangeeons.plugins.*; no app dependencies. Plug-ins compile against
// this module; the strange-eons-app module implements its types.

plugins {
    `java-library`
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
