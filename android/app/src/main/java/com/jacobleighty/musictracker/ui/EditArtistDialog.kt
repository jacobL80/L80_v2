package com.jacobleighty.musictracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jacobleighty.musictracker.data.Artist

@Composable
fun PasswordDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Mode") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (password.isNotBlank()) onConfirm(password) }, enabled = password.isNotBlank()) {
                Text("Enter")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EditArtistDialog(
    artist: Artist,
    allArtists: List<Artist>,
    onSave: (Artist) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var form by remember { mutableStateOf(artist) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isNew = artist.id == 0
    val scroll = rememberScrollState()
    var nameExpanded by remember { mutableStateOf(false) }
    val nameSuggestions = remember(form.name, allArtists) {
        if (form.name.isBlank()) emptyList()
        else allArtists.filter { it.name.contains(form.name, ignoreCase = true) }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(if (isNew) "Add Artist" else "Edit Artist", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = {
                            form = form.copy(name = it)
                            nameExpanded = true
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    if (nameExpanded && nameSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(4.dp)),
                        ) {
                            nameSuggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        form = form.copy(name = suggestion.name)
                                        nameExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(suggestion.name, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = form.albumTitle,
                    onValueChange = { form = form.copy(albumTitle = it) },
                    label = { Text("Album Title") },
                    placeholder = { Text("leave blank if unknown") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.nextRelease,
                        onValueChange = { form = form.copy(nextRelease = it) },
                        label = { Text("Next Release") },
                        placeholder = { Text("M/D/YY, M/D/YYYY, or M/D") },
                        isError = form.nextRelease.isNotBlank() && !DateUtils.isValidDate(form.nextRelease),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.lastRelease,
                        onValueChange = { form = form.copy(lastRelease = it) },
                        label = { Text("Last Release") },
                        placeholder = { Text("YYYY") },
                        isError = form.lastRelease.isNotBlank() && !DateUtils.isValidDate(form.lastRelease),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = form.incompleteCollection, onCheckedChange = { form = form.copy(incompleteCollection = it) })
                        Text("Incomplete collection")
                    }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = form.hiatus, onCheckedChange = { form = form.copy(hiatus = it) })
                        Text("Hiatus")
                    }
                }
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { form = form.copy(notes = it) },
                    label = { Text("Notes") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.url,
                    onValueChange = { form = form.copy(url = it) },
                    label = { Text("URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isNew) {
                    if (!confirmDelete) {
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Delete Artist") }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Are you sure?", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDelete(form.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Yes, delete") }
                            TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (form.name.isNotBlank()) onSave(form) }, enabled = form.name.isNotBlank() && DateUtils.isValidDate(form.nextRelease) && DateUtils.isValidDate(form.lastRelease)) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
