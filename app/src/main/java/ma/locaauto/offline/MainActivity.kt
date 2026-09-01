package ma.locaauto.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ma.locaauto.offline.ui.LocaAutoTheme
import ma.locaauto.offline.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rentalViewModel: RentalViewModel = viewModel()
            LocaAutoTheme { MainScreen(rentalViewModel) }
        }
    }
}
