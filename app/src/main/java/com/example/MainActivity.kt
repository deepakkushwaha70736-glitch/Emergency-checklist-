package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.audio.EmergencySoundPlayer
import com.example.data.EmergencyRepository
import com.example.ui.EmergencyControlApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val soundPlayer = EmergencySoundPlayer(applicationContext)
        val repository = EmergencyRepository(applicationContext, soundPlayer)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EmergencyControlApp(repository = repository)
                }
            }
        }
    }
}
