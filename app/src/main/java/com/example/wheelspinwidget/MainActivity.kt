package com.example.wheelspinwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bez.spinwheel_sdk.data.work.ConfigSyncWorker
import com.bez.spinwheel_sdk.presentation.SpinActivity
import com.example.wheelspinwidget.ui.theme.WheelSpinWidgetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WheelSpinWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tapp Spin Wheel",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            startActivity(android.content.Intent(this@MainActivity, SpinActivity::class.java))
                        }) {
                            Text("Open Spin Wheel")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            WorkManager.getInstance(this@MainActivity)
                                .enqueue(OneTimeWorkRequestBuilder<ConfigSyncWorker>().build())
                        }) {
                            Text("Simulate Config Push (FCM)")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"Simulate Config Push\" picks a new random duration\n(1000 / 2500 / 5000 ms). Reopen the wheel to see it.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
