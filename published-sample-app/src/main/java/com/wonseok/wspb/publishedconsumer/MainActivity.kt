package com.wonseok.wspb.publishedconsumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wonseok.wspb.TestPreference
import com.wonseok.wspb.UserPreference
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class PublishedUserData(
    val id: Int,
    val name: String,
)

@WSProto(name = "test_preference")
data class PublishedTestData(
    val test: String,
)

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
        .setId(2)
        .setName("published")
        .build()
    val testPreference = TestPreference.newBuilder()
        .setTest("maven-ready")
        .build()

    Text(
        text = "${userPreference.name} / ${testPreference.test}"
    )
}
