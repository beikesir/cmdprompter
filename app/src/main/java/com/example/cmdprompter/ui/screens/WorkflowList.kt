package com.example.cmdprompter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.data.model.Workflow
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.TextGray

/** 工作流选项卡：列出所有工作流，点击进入编辑 */
@Composable
fun WorkflowList(
    workflows: List<Workflow>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(workflows, key = { it.id }) { wf ->
            WorkflowCard(
                workflow = wf,
                onOpen = { onOpen(wf.id) },
                onDelete = { onDelete(wf.id) }
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun WorkflowCard(
    workflow: Workflow,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\uD83D\uDD00",
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(
                text = workflow.name,
                fontSize = 12.5f.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${workflow.steps.size} 步",
                fontSize = 9.sp,
                color = TextGray
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDDD1\uFE0F", fontSize = 12.sp)
            }
        }

        if (workflow.sourceGroupName.isNotBlank()) {
            Text(
                text = "基于：${workflow.sourceGroupName}",
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (workflow.note.isNotBlank()) {
            Text(
                text = workflow.note,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF555555),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 步骤预览
        if (workflow.steps.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F6F8), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                workflow.steps.take(3).forEachIndexed { i, step ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "${i + 1}.",
                            fontSize = 9.sp,
                            color = TextGray,
                            modifier = Modifier.width(14.dp)
                        )
                        Text(
                            text = step.preview().ifBlank { step.name },
                            fontSize = 9.5f.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1F2933),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (workflow.steps.size > 3) {
                    Text(
                        text = "…还有 ${workflow.steps.size - 3} 步",
                        fontSize = 9.sp,
                        color = TextGray,
                        modifier = Modifier.padding(start = 14.dp, top = 1.dp)
                    )
                }
            }
        }
    }
}
