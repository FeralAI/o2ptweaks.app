package dev.adrian.thortools.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_WIDTH = 150.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    modifier: Modifier = Modifier,
    context: Context,
    options: List<String>,
    value: String? = "",
    width: Dp = DEFAULT_WIDTH,
    onSelected: (String) -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.width(width)
    ) {
        ExposedDropdownMenuBox(
            expanded = isOpen,
            onExpandedChange = {
                isOpen = !isOpen
            }
        ) {
            TextField(
                value = value ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOpen) },
                modifier = modifier.menuAnchor(),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            ExposedDropdownMenu(
                expanded = isOpen,
                onDismissRequest = { isOpen = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            isOpen = false
                            onSelected(option)
                        }
                    )
                }
            }
        }
    }
}