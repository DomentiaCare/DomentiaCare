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
fun DMT_Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFF49000), // 기본 주황색
    // enable 설정
    enabled: Boolean = true // 버튼 활성화 여부
) {

    val colors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
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
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
