import com.google.protobuf.gradle.ProtobufExtension
import com.wonseok.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class WSProtoBufConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply(libs.findPlugin("protobuf").get().get().pluginId)
            }

            configure<ProtobufExtension> {
                protoc {
                    artifact = libs.findLibrary("protobuf-protoc").get().get().toString()
                }

                generateProtoTasks {
                    all().forEach { task ->
                        task.builtins {
                            register("kotlin") {
                                option("lite")
                            }
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
}