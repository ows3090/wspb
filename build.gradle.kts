import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.spotless) apply true
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    apply<SpotlessPlugin>()
    configure<SpotlessExtension> {
        kotlin {
            ktlint().apply {
                editorConfigOverride(
                    mapOf(
                        "trailing-comma" to "disabled",
                        "experimental:trailing-comma-on-declaration-site" to "disabled",
                        "experimental:trailing-comma-on-call-site" to "disabled"
                    )
                )
            }
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}