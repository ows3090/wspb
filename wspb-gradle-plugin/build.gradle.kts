plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

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
    plugins {
        register("wspbConventionPlugin") {
            id =
                libs.plugins.wspb.proto
                    .get()
                    .pluginId
            implementationClass = "com.wonseok.wspb.gradle.plugin.WSPBConventionPlugin"
        }
    }
}

group = "com.wonseok.wspb.plugins"
version = "1.0.0"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "wspb-gradle-plugin"
        }
    }
}
