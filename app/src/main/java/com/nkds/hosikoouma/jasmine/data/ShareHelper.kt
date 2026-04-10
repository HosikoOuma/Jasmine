package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nkds.hosikoouma.jasmine.datamodels.Track

object ShareHelper {
    fun shareTrack(context: Context, track: Track) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share track"))
    }

    fun shareTracks(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        
        if (tracks.size == 1) {
            shareTrack(context, tracks.first())
            return
        }

        val uris = ArrayList<Uri>().apply {
            tracks.forEach { add(it.contentUri) }
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share tracks"))
    }
}
