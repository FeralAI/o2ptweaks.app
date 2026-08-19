package dev.adrian.thortools.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LABEL_PADDING = PaddingValues(0.dp, 0.dp, 16.dp, 4.dp)
private val ROW_PADDING = PaddingValues(16.dp, 4.dp)

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    label: String,
    detail: String? = null,
    notice: String? = null,
    control: @Composable () -> Unit,
) {
    HorizontalDivider(modifier = modifier.padding(PaddingValues(0.dp, 8.dp)))

    Row(
        modifier = modifier.fillMaxWidth().padding(ROW_PADDING),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            modifier = modifier.weight(1.0f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                modifier = modifier.fillMaxWidth().padding(LABEL_PADDING),
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (detail != null) {
                Text(
                    modifier = modifier.fillMaxWidth().padding(LABEL_PADDING),
                    style = MaterialTheme.typography.bodySmall,
                    text = detail,
                )
            }
            Spacer(modifier = modifier.padding(6.dp))
            if (notice != null) {
                Text(
                    modifier = modifier.fillMaxWidth().padding(LABEL_PADDING),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(200, 0, 0),
                    fontWeight = FontWeight.Bold,
                    text = notice,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top,
        ) {
            control()
        }
    }
}