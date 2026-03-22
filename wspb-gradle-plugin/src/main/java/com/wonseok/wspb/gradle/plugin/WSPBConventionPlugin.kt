package com.wonseok.wspb.gradle.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.proto
import com.wonseok.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class WSPBConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply(libs.findPlugin("protobuf").get().get().pluginId)

                withId(libs.findPlugin("android-application").get().get().pluginId) {
                    configureProtoSourceSets()
                }

                withId(libs.findPlugin("android-library").get().get().pluginId) {
                    configureProtoSourceSets()
                }
            }

            configure<ProtobufExtension> {
                protoc {
                    artifact = libs.findLibrary("protobuf-protoc").get().get().toString()
                }

                generateProtoTasks {
                    all().forEach { task ->
                        task.builtins {
                            register("java") {
                                option("lite")
                            }
                        }
                    }
                }
            }

            dependencies {
                add("implementation", libs.findLibrary("protobuf-kotlin-lite").get())
            }
        }
    }

    private fun Project.configureProtoSourceSets() {
        extensions.configure<AndroidComponentsExtension<*, *, *>>("androidComponents") {
            onVariants { variant ->
                val protoDir =
                    layout.buildDirectory.dir("generated/ksp/${variant.name}/resources")

                extensions.configure<BaseExtension>("android") {
                    sourceSets.getByName(variant.name) {
                        proto {
                            srcDir(protoDir)
                        }
                    }
                }
            }
        }
    }
}
