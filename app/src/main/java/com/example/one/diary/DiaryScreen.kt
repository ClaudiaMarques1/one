package com.example.one.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.one.ui.theme.OneTheme

@Composable
fun DiaryScreen(viewModel: DiaryViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsState()
    var text by remember { mutableStateOf("") }

    DiaryScreenContent(
        entries = entries,
        text = text,
        onTextChange = { text = it },
        onAddClick = {
            viewModel.addEntry(text)
            text = ""
        },
        onDeleteClick = { viewModel.deleteEntry(it) }
    )
}

@Composable
private fun DiaryScreenContent(
    entries: List<DiaryEntry>,
    text: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("New entry") }
                )
                Button(onClick = onAddClick) {
                    Text("Add")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.date)
                            Text(entry.text)
                        }
                        IconButton(onClick = { onDeleteClick(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryScreenPreview() {
    OneTheme {
        DiaryScreenContent(
            entries = listOf(
                DiaryEntry(id = "1", date = "2026-09-12", text = "Leg day, felt strong."),
                DiaryEntry(id = "2", date = "2026-09-11", text = "Rest day, meal prepped for the week.")
            ),
            text = "",
            onTextChange = {},
            onAddClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun DiaryScreenEmptyPreview() {
    OneTheme {
        DiaryScreenContent(
            entries = emptyList(),
            text = "",
            onTextChange = {},
            onAddClick = {},
            onDeleteClick = {}
        )
    }
}
