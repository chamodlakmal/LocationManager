package lk.chamiviews.locationmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import lk.chamiviews.locationmanager.presentation.screen.MainDemoScreen
import lk.chamiviews.locationmanager.ui.theme.LocationManagerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LocationManagerTheme {
                MainDemoScreen()
            }
        }
    }
}
