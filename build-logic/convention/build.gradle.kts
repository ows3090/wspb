plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.protobuf.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("wonseokProtoBuf") {
            id = libs.plugins.wspb.proto.get().pluginId
            implementationClass = "WSProtoBufConventionPlugin"
        }
    }
}
