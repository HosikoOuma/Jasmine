package com.nkds.hosikoouma.jasmine.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.screens.PlaylistCoverEditor

@Composable
fun TrackEditDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String, cover: Bitmap?) -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            selectedImageUri = it
            showEditor = true
        }
    }

    if (showEditor && selectedImageUri != null) {
        // Оборачиваем редактор в полноэкранный Dialog, 
        // чтобы он был ПОВЕРХ BottomSheet и других окон.
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PlaylistCoverEditor(
                imageUri = selectedImageUri!!,
                onDismiss = { showEditor = false },
                onConfirm = { bitmap ->
                    editedBitmap = bitmap
                    showEditor = false
                }
            )
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.edit_track_info)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        if (editedBitmap != null) {
                            Image(
                                bitmap = editedBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AlbumArt(albumArtUri = track.albumArtUri, modifier = Modifier.fillMaxSize())
                        }
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text(stringResource(R.string.artist)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = album,
                        onValueChange = { album = it },
                        label = { Text(stringResource(R.string.album)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(title, artist, album, editedBitmap)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
