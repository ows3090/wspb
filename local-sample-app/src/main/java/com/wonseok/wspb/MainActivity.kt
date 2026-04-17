package com.wonseok.wspb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String,
)

@WSProto(name = "test_preference")
data class TestData(
    val test: String,
)

/**
 * Minimal Android entry point used only as an integration harness for the
 * local-consumer flow in this repository.
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
    // These builders are generated from the @WSProto models declared above.
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
