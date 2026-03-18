package com.emilio.notificationmanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.emilio.notificationmanager.AppInfo

@Composable
fun AppListItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    isBlocked: Boolean = false,
    actionText: String = "",
    expirationTime: Long = 0L,
    rightContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.icon) {
            try {
                app.icon.toBitmap(width = 144, height = 144).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
            if (isBlocked && expirationTime > System.currentTimeMillis()) {
                val remainingMinutes = ((expirationTime - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                Text(
                    text = "$actionText (${remainingMinutes} min restantes)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        if (rightContent != null) {
            rightContent()
        }
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    }
    }
}
