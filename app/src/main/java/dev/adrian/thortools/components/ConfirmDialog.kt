package dev.adrian.thortools.components

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmDialog (
    modifier: Modifier,
    dialogText: String? = null,
    dialogTitle: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    onConfirmation: (() -> Unit)? = null,
) {
    AlertDialog(
        title = {
            if (dialogTitle != null) {
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = dialogTitle, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        text = {
            if (dialogText != null) {
                Column(
                    modifier = modifier.verticalScroll(
                        state = rememberScrollState(),
                        flingBehavior = ScrollableDefaults.flingBehavior()
                    ).fillMaxHeight().fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = dialogText ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        onDismissRequest = {
            if (onDismissRequest != null)
                onDismissRequest()
        },
        confirmButton = {
            if (onConfirmation != null) {
                TextButton(
                    onClick = {
                        onConfirmation()
                    }
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (onDismissRequest != null) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    },
                ) {
                    Text(
                        text = "Dismiss",
                        color = Color(200, 0, 0),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    )
}