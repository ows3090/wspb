package com.wonseok.wspb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wonseok.wspb.annotation.WSProto

/**
 * Sample input model used by the processor.
 *
 * The annotation tells `wspb` to generate `user_preference.proto`, which the
 * protobuf Gradle plugin later turns into the `UserPreference` Java lite class.
 */
@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String
)

/**
 * Second sample model that proves multiple generated schema files can coexist
 * in the same project.
 */
@WSProto(name = "test_preference")
data class TestData(
    val test: String
)

/**
 * Minimal Android entry point used only as an integration harness for the
 * library modules in this repository.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }
    }
}

@Composable
private fun MainScreen() {
    // These builders come from Java lite classes generated from the `.proto`
    // files produced by the processor. Referencing them here proves that the
    // full pipeline works inside a real Android module.
    val userPreference = UserPreference.newBuilder()
        .setId(1)
        .setName("wonseok")
        .build()
    val testPreference = TestPreference.newBuilder()
        .setTest("proto-ready")
        .build()

    Text(
        // Keep the UI intentionally small. The point of the sample is to show
        // successful code generation and consumption, not app features.
        text = "${userPreference.name} / ${testPreference.test}"
    )
}
