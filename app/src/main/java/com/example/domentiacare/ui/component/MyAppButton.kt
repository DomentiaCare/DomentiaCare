package com.example.domentiacare.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.theme.GrayDisabled

@Composable
fun MyAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // 👈 기본값을 Modifier로 지정!
) {

    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = GrayDisabled,
        disabledContentColor = Color.White.copy(alpha = 0.6f)
    )


    Button(
        onClick = onClick,
        modifier = modifier, // 외부에서 받은 modifier 사용
        colors = colors,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}
