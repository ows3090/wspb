package com.wonseok.wsprotobuf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wonseok.wsprotobuf.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String
)

@WSProto(name = "test_preference")
data class TestData(
    val test: String
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
    Text(
        text = "Hello world"
    )
}