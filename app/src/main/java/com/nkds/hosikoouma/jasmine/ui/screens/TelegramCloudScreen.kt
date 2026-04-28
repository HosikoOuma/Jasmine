package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.data.TelegramChannelEntity
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.ExpressiveSyncIndicator
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramCloudViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramCloudScreen(
    navController: NavController,
    viewModel: TelegramCloudViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    
    var channelToDelete by remember { mutableStateOf<TelegramChannelEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Search Public Channel (@username)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            trailingIcon = {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (state.isSearching) {
                        ExpressiveSyncIndicator(size = 28.dp)
                    } else {
                        IconButton(onClick = { viewModel.searchChannel(username) }) {
                            Icon(Icons.Rounded.Search, null)
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        // New Button: Select from my chats
        Button(
            onClick = { navController.navigate("telegram_chat_picker") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Select from my chats")
        }

        // Search Result
        state.searchResult?.let { chat ->
            val isAdding = state.syncingChannels.contains(chat.id)
            Card(
                modifier = Modifier.padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                ListItem(
                    headlineContent = { Text(chat.title, fontWeight = FontWeight.Bold) },
                    supportingContent = { 
                        Text(if (isAdding) "Adding tracks..." else "Click to add this channel")
                    },
                    leadingContent = {
                        Surface(Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Rounded.Group, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    trailingContent = {
                        Box(modifier = Modifier.widthIn(min = 80.dp), contentAlignment = Alignment.Center) {
                            if (isAdding) {
                                ExpressiveSyncIndicator(size = 32.dp)
                            } else {
                                Button(onClick = { viewModel.addChannel(chat) }) {
                                    Text("Add")
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Text(
            "My Cloud Library",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp),
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            if (channels.isEmpty()) {
                item {
                    Text(
                        text = "No channels added yet", 
                        modifier = Modifier.fillMaxWidth(), 
                        textAlign = TextAlign.Center, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(channels, key = { it.chatId }) { channel ->
                val isSyncing = state.syncingChannels.contains(channel.chatId)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            navController.navigate("telegram_channel_detail/${channel.chatId}") 
                        },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(channel.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${channel.songCount} songs") },
                        leadingContent = {
                            val photoUri = channel.photoPath?.let { Uri.fromFile(File(it)) }
                            AlbumArt(
                                albumArtUri = photoUri,
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    if (isSyncing) {
                                        ExpressiveSyncIndicator(size = 32.dp)
                                    } else {
                                        IconButton(onClick = { viewModel.syncChannel(channel.chatId) }) {
                                            Icon(Icons.Rounded.Sync, null)
                                        }
                                    }
                                }

                                IconButton(onClick = { channelToDelete = channel }) {
                                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("Delete Channel") },
            text = { Text("Are you sure you want to remove \"${channelToDelete?.title}\" and all its tracks from your cloud library?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelToDelete?.let { viewModel.removeChannel(it.chatId) }
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
