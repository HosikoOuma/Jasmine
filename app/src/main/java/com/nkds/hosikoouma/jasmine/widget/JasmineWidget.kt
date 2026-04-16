package com.nkds.hosikoouma.jasmine.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.nkds.hosikoouma.jasmine.R
import java.io.InputStream

class JasmineWidget : GlanceAppWidget() {
    override val stateDefinition = JasmineWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<JasmineWidgetState>()
            GlanceTheme {
                JasmineWidgetContent(state)
            }
        }
    }

    @Composable
    private fun JasmineWidgetContent(state: JasmineWidgetState) {
        val context = LocalContext.current
        val albumArt = state.albumArtUri?.let { uriString ->
            loadBitmapFromUri(context, uriString)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .cornerRadius(18.dp)
                //.padding(4.dp)
        ) {
            // Album Art or Placeholder
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(14.dp)
                    .background(ColorProvider(Color.Black))
            ) {
                if (albumArt != null) {
                    Image(
                        provider = ImageProvider(albumArt),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ison_vec),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Overlay when feedback is shown
                if (state.showFeedback) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (state.isPlaying) R.drawable.ic_widget_pause 
                                else R.drawable.ic_widget_play
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier.size(56.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White))
                        )
                    }
                }

                // Equalizer Icon (representation for Widget)
                if (state.isPlaying && !state.showFeedback) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        WidgetEqualizerIcon()
                    }
                }
            }

            // Clickable layer covering the whole widget
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionRunCallback<TogglePlayPauseCallback>())
            ) {}
        }
    }

    @Composable
    private fun WidgetEqualizerIcon() {
        Row(
            modifier = GlanceModifier.size(height = 16.dp, width = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = GlanceModifier.size(width = 3.dp, height = 10.dp).background(Color.White).cornerRadius(2.dp)) {}
            Box(modifier = GlanceModifier.size(width = 2.dp, height = 1.dp)) {} // Spacer
            Box(modifier = GlanceModifier.size(width = 3.dp, height = 16.dp).background(Color.White).cornerRadius(2.dp)) {}
            Box(modifier = GlanceModifier.size(width = 2.dp, height = 1.dp)) {} // Spacer
            Box(modifier = GlanceModifier.size(width = 3.dp, height = 7.dp).background(Color.White).cornerRadius(2.dp)) {}
        }
    }

    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Optimized for widget
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            null
        }
    }
}

class TogglePlayPauseCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = android.content.Intent(context, com.nkds.hosikoouma.jasmine.PlaybackService::class.java).apply {
            action = "ACTION_WIDGET_PLAY_PAUSE"
        }
        context.startService(intent)
    }
}
