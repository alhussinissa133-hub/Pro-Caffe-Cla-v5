package com.cafe.accounts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafe.accounts.ui.AppViewModel
import com.cafe.accounts.ui.BeanBackground
import com.cafe.accounts.ui.MainScreen
import com.cafe.accounts.ui.TricolorStripe
import kotlinx.coroutines.delay

val Roast = Color(0xFF3E2723)
val Crema = Color(0xFFD7B98E)
val Latte = Color(0xFFF5E6D3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashFile = java.io.File(filesDir, "crash.txt")
        if (crashFile.exists()) {
            val trace = try { crashFile.readText() } catch (_: Throwable) { "?" }
            setContent {
                var show by remember { mutableStateOf(true) }
                if (show) {
                    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("حصل خطأ بالمرة السابقة — صوّر هالشاشة وابعتها", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Button({ crashFile.delete(); show = false; recreate() }) { Text("مسح ومتابعة") }
                        Spacer(Modifier.height(8.dp))
                        Text(trace.take(3500), fontSize = 11.sp)
                    }
                }
            }
            return
        }
        setContent {
            val scheme = lightColorScheme(
                primary = Color(0xFF5D4037), onPrimary = Color.White,
                secondary = Color(0xFFB08968), background = Color(0xFFFFF8F0), surface = Color.White,
                primaryContainer = Latte, onPrimaryContainer = Roast,
                secondaryContainer = Crema, onSecondaryContainer = Roast,
                surfaceContainer = Latte, surfaceContainerHighest = Color(0xFFFFFBF5),
                surfaceVariant = Latte
            )
            MaterialTheme(colorScheme = scheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val vm: AppViewModel = viewModel()
                    var splash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) { delay(2200); splash = false }
                    if (splash) Splash() else MainScreen(vm)
                }
            }
        }
    }
}

@Composable
fun Splash() {
    BeanBackground(dark = true, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(150.dp))
            Spacer(Modifier.height(16.dp))
            Text("Pro Caffe", color = Latte, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TricolorStripe(Modifier.width(110.dp), 4.dp)
            Spacer(Modifier.height(8.dp))
            Text("Caffè • Espresso • Cappuccino", color = Crema, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("محاسبة البيع والشراء والديون", color = Crema, fontSize = 14.sp)
        }
        Text(
            "Hussin Issa", color = Crema, fontSize = 20.sp,
            fontFamily = FontFamily.Cursive, fontStyle = FontStyle.Italic,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
        )
    }
}
