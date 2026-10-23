rootProject.name = "strange-eons"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    ":vendor",
    ":strange-eons-plugin-api",
    ":strange-eons-script-api",
    ":strange-eons-conventional-components",
    ":strange-eons-app",
)
