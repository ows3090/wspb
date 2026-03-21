import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.spotless) apply true
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}

description = "Generate Protobuf schemas and Android build wiring from Kotlin models."

val publishableModules = setOf(
    ":wspb-annotation",
    ":wspb-processor",
    ":wspb-gradle-plugin",
)

val centralPublishableModules = setOf(
    ":wspb-annotation",
    ":wspb-processor",
)

val projectGradleProperties = Properties().apply {
    val file = rootProject.file(".gradle/gradle.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun Project.projectGradleProperty(name: String) =
    providers.provider { projectGradleProperties.getProperty(name) }

fun Project.isRemotePublishRequested(): Boolean =
    gradle.startParameter.taskNames.any { taskName ->
        val normalized = taskName.lowercase()
        ("publish" in normalized && "mavenlocal" !in normalized) || normalized == "publishplugins"
    }

subprojects {
    if (path in publishableModules) {
        group = providers.gradleProperty("POM_GROUP").get()
        version = providers.gradleProperty("WSPB_VERSION").get()
    }

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

    pluginManager.withPlugin("java") {
        if (path in publishableModules) {
            extensions.configure<JavaPluginExtension> {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }

    pluginManager.withPlugin("maven-publish") {
        if (path in publishableModules) {
            extensions.configure<PublishingExtension> {
                if (path in centralPublishableModules) {
                    repositories {
                        maven {
                            name = "sonatypeCentral"
                            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                            credentials {
                                // Release credentials are loaded from ./.gradle/gradle.properties.
                                username = projectGradleProperty("CENTRAL_PORTAL_USERNAME").orNull
                                password = projectGradleProperty("CENTRAL_PORTAL_PASSWORD").orNull
                            }
                        }
                    }
                }

                publications.withType(MavenPublication::class.java).configureEach {
                    pom {
                        name.set(project.name)
                        description.set(project.description ?: rootProject.description)
                        url.set(providers.gradleProperty("POM_URL"))
                        licenses {
                            license {
                                name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                                url.set(providers.gradleProperty("POM_LICENSE_URL"))
                                distribution.set(providers.gradleProperty("POM_LICENSE_DIST"))
                            }
                        }
                        developers {
                            developer {
                                id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                                name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                                url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                            }
                        }
                        scm {
                            url.set(providers.gradleProperty("POM_SCM_URL"))
                            connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                        }
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("signing") {
        if (path in publishableModules) {
            val signingKey = projectGradleProperty("SIGNING_KEY").orNull
            val signingPassword = projectGradleProperty("SIGNING_PASSWORD").orNull
            val remotePublishRequested = isRemotePublishRequested()

            extensions.configure<SigningExtension> {
                isRequired = remotePublishRequested

                if (!signingKey.isNullOrBlank()) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign(extensions.getByType(PublishingExtension::class.java).publications)
                }
            }
        }
    }
}
