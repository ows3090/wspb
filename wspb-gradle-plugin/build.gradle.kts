plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.plugin.publish)
}

description = "Gradle plugin that wires generated proto sources into Android builds."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.protobuf.gradlePlugin)
}

gradlePlugin {
    website = providers.gradleProperty("POM_URL").get()
    vcsUrl = providers.gradleProperty("POM_SCM_URL").get()

    plugins {
        register("wspbConventionPluginLegacy") {
            id = providers.gradleProperty("WSPB_LEGACY_PLUGIN_ID").get()
            displayName = "wspb proto plugin (legacy id)"
            description = project.description
            tags.set(listOf("protobuf", "ksp", "android"))
            implementationClass = "com.wonseok.wspb.gradle.plugin.WSPBConventionPlugin"
        }

        register("wspbConventionPlugin") {
            id = providers.gradleProperty("WSPB_PLUGIN_ID").get()
            displayName = "wspb proto plugin"
            description = project.description
            tags.set(listOf("protobuf", "ksp", "android"))
            implementationClass = "com.wonseok.wspb.gradle.plugin.WSPBConventionPlugin"
        }
    }
}
