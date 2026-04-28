package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.ExpressiveSyncIndicator
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramCloudViewModel
import org.drinkless.tdlib.TdApi
import java.io.File

enum class ChatFilter { ALL, CHANNELS, GROUPS, BOTS, PRIVATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramChatPickerScreen(
    navController: NavController,
    viewModel: TelegramCloudViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }

    LaunchedEffect(Unit) {
        viewModel.loadMyChats()
    }

    val filteredChats = remember(state.myChats, selectedFilter, searchQuery) {
        state.myChats.filter { chat ->
            val matchesSearch = chat.title.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                ChatFilter.ALL -> true
                ChatFilter.CHANNELS -> chat.type is TdApi.ChatTypeSupergroup && (chat.type as TdApi.ChatTypeSupergroup).isChannel
                ChatFilter.GROUPS -> chat.type is TdApi.ChatTypeBasicGroup || (chat.type is TdApi.ChatTypeSupergroup && !(chat.type as TdApi.ChatTypeSupergroup).isChannel)
                ChatFilter.BOTS -> chat.type is TdApi.ChatTypePrivate 
                ChatFilter.PRIVATE -> chat.type is TdApi.ChatTypePrivate
            }
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filters
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchMyChats(it)
                },
                placeholder = { Text("Search in my chats...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.padding(vertical = 12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (state.isFetchingChats && state.myChats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                if (filteredChats.isEmpty()) {
                    item {
                        Text(
                            "No chats found",
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(filteredChats) { chat ->
                    val isAdding = state.syncingChannels.contains(chat.id)
                    ListItem(
                        headlineContent = { Text(chat.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { 
                            val typeText = when (chat.type) {
                                is TdApi.ChatTypePrivate -> "Private"
                                is TdApi.ChatTypeBasicGroup -> "Group"
                                is TdApi.ChatTypeSupergroup -> if ((chat.type as TdApi.ChatTypeSupergroup).isChannel) "Channel" else "Supergroup"
                                else -> "Chat"
                            }
                            Text(typeText) 
                        },
                        leadingContent = {
                            val photoPath = chat.photo?.small?.local?.path
                            val photoUri = if (!photoPath.isNullOrEmpty() && File(photoPath).exists()) {
                                Uri.fromFile(File(photoPath))
                            } else null
                            
                            AlbumArt(
                                albumArtUri = photoUri,
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape
                            )
                        },
                        trailingContent = {
                            if (isAdding) {
                                ExpressiveSyncIndicator(size = 24.dp)
                            } else {
                                IconButton(onClick = { 
                                    viewModel.addChannel(chat)
                                    navController.popBackStack()
                                }) {
                                    Icon(Icons.Rounded.Add, null)
                                }
                            }
                        },
                        modifier = Modifier.clickable { 
                            viewModel.addChannel(chat)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
