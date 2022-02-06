package com.pke.slope_helper

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pke.slope_helper.ui.theme.SlopehelperTheme
import com.pke.slope_helper.viewmodel.BleViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState


class MainActivity : AppCompatActivity() {
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var context = this.applicationContext
        setContent {
            SlopehelperTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val bleService = remember { BleViewModel(context = context ) }
                        bleService.startStopScan()
                        Main(bleService)
                    }
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun Main(bleService: BleViewModel) {
    val multiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    )
    val modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp)
    Column(Modifier.fillMaxWidth().padding(start=20.dp, end=20.dp), horizontalAlignment = Alignment.Start) {
        Spacer(modifier = modifier.size(40.dp, 40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scan Status:  ", fontWeight = FontWeight.SemiBold)
            Text(bleService.scanningStatus)
        }
        Spacer(modifier = modifier.size(40.dp, 40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Connection Status:  ", fontWeight = FontWeight.SemiBold)
            Text(bleService.connectionStatus)
        }
        Spacer(modifier = modifier.size(40.dp, 40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Data read Status:  ", fontWeight = FontWeight.SemiBold)
        }
        Text(bleService.connectionStatus)

        Spacer(modifier = modifier.size(40.dp, 200.dp))
        Row() {
            Spacer(modifier = modifier.size(60.dp, 40.dp))
            TextButton(
                onClick = { multiplePermissionsState.launchMultiplePermissionRequest(); },
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color(0xFFFFFFFF),
                ),
            ) {
                Text("Grant Permissions")
            }
            Spacer(modifier = modifier.size(60.dp, 40.dp))
        }

    }
}
