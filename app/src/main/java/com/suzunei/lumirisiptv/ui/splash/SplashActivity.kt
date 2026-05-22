package com.suzunei.lumirisiptv.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.suzunei.lumirisiptv.R
import com.suzunei.lumirisiptv.data.repository.PlaylistRepository
import com.suzunei.lumirisiptv.ui.main.MainActivity
import com.suzunei.lumirisiptv.ui.theme.LumirisTheme
import com.suzunei.lumirisiptv.util.applyImmersiveLandscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplashActivity : ComponentActivity() {

    private val repository = PlaylistRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyImmersiveLandscape()
        setContent {
            LumirisTheme {
                SplashScreen()
                LaunchedEffect(Unit) {
                    // Best-effort prefetch; even on failure MainActivity will surface the error and
                    // let the user retry via the refresh button.
                    runCatching { withContext(Dispatchers.IO) { repository.loadPlaylist() } }
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.lumiris_logo),
                contentDescription = "Suzunei",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Suzunei IPTV",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Memuatkan saluran…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
