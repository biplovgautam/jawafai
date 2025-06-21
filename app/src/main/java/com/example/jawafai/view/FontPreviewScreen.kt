package com.example.jawafai.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FontPreviewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Title Large (Bold)", style = MaterialTheme.typography.titleLarge)
        Text("Body Large (Normal)", style = MaterialTheme.typography.bodyLarge)
        Text("Label Small (Medium)", style = MaterialTheme.typography.labelSmall)
    }
}