package com.cafe.accounts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cafe.accounts.Crema
import com.cafe.accounts.Latte
import com.cafe.accounts.Roast

val Cream = Color(0xFFFFF8F0)
val Mocha = Color(0xFF5D4037)
val ItalyGreen = Color(0xFF009246)
val ItalyRed = Color(0xFFCE2B37)

/** Warm cream (or dark roast) background with a faint scattered coffee-bean pattern. */
@Composable
fun BeanBackground(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val base = if (dark) Brush.verticalGradient(listOf(Roast, Color(0xFF2B1A16)))
               else Brush.verticalGradient(listOf(Cream, Latte))
    val bean = if (dark) Crema.copy(alpha = 0.08f) else Mocha.copy(alpha = 0.07f)
    val crease = if (dark) Color(0xFF2B1A16).copy(alpha = 0.7f) else Cream.copy(alpha = 0.9f)
    Box(
        modifier.fillMaxSize().background(base).drawBehind {
            val step = 78.dp.toPx()
            val w = 24.dp.toPx()
            val h = 15.dp.toPx()
            var row = 0
            var y = 0f
            while (y < size.height + step) {
                var x = if (row % 2 == 0) 0f else step / 2
                var col = 0
                while (x < size.width + step) {
                    val angle = ((row * 37 + col * 53) % 90) - 45f
                    rotate(angle, Offset(x, y)) {
                        drawOval(bean, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h))
                        drawLine(crease, Offset(x - w / 2 + 4.dp.toPx(), y), Offset(x + w / 2 - 4.dp.toPx(), y), strokeWidth = 1.5.dp.toPx())
                    }
                    x += step
                    col++
                }
                y += step * 0.75f
                row++
            }
        },
        contentAlignment = contentAlignment,
        content = content
    )
}

/** Italian flag stripe (green / white / red), always left-to-right. */
@Composable
fun TricolorStripe(modifier: Modifier = Modifier.fillMaxWidth(), height: Dp = 4.dp) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier.height(height)) {
            listOf(ItalyGreen, Color.White, ItalyRed).forEach {
                Box(Modifier.weight(1f).fillMaxHeight().background(it))
            }
        }
    }
}
