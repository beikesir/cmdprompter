package com.example.cmdprompter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.SegmentBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 搜索滑条（F8）：默认隐藏，展开动画到 38dp。
 */
@Composable
fun SearchSlide(
    expanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(expanded) {
                if (expanded) runCatching { focusRequester.requestFocus() }
            }
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF222222)),
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(SegmentBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = "搜索名称、描述或标签…", fontSize = 13.sp, color = TextGray)
                }
                inner()
            }
        }
    )
}
