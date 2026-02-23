package com.innosage.androidagentictemplate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innosage.androidagentictemplate.data.UtteranceEntity
import com.innosage.androidagentictemplate.ui.theme.ElectricGreen
import com.innosage.androidagentictemplate.ui.theme.SoftPurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UtteranceFeed(
    utterances: List<UtteranceEntity>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(utterances.size) {
        if (utterances.isNotEmpty()) {
            listState.animateScrollToItem(utterances.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(utterances) { utterance ->
            UtteranceBubble(utterance)
        }
    }
}

@Composable
fun UtteranceBubble(utterance: UtteranceEntity) {
    val intentColor = when (utterance.intentLabel?.lowercase()) {
        "decision" -> SoftPurple
        "blocker" -> Color(0xFFE57373) // Soft Red
        "idea" -> ElectricGreen
        else -> MaterialTheme.colorScheme.secondary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = utterance.transcript,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!utterance.intentLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = utterance.intentLabel.uppercase(),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = intentColor
                        ),
                        border = null
                    )
                }
            }
        }
        
        Text(
            text = formatTime(utterance.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
