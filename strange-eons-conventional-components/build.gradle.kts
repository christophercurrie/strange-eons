// Conventional component framework SPI. Builds on the plug-in API surface to
// expose ComponentHandler / Face / EditorPanel for component-rendering
// plug-ins.

plugins {
    `java-library`
}

dependencies {
    api(project(":strange-eons-plugin-api"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
