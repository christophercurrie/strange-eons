// Conventional component framework SPI. Builds on the plug-in API surface to
// expose ComponentHandler / Face / EditorPanel for component-rendering
// plug-ins.

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        name = "Strange Eons Conventional Components"
        description = "Component framework SPI for Strange Eons component-rendering plug-ins."
    }
}

dependencies {
    api(project(":strange-eons-plugin-api"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
