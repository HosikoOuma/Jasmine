package com.nkds.hosikoouma.jasmine.data.telegram

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import javax.inject.Inject

class TelegramArtFetcher(
    private val context: Context,
    private val data: Uri,
    private val repository: TelegramRepository
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val chatId = data.host?.toLongOrNull() ?: return null
        val messageId = data.pathSegments.firstOrNull()?.toLongOrNull() ?: return null
        
        val file = repository.getArtworkFile(chatId, messageId) ?: return null
        
        return SourceResult(
            source = ImageSource(file.source().buffer(), context),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK
        )
    }

    class Factory @Inject constructor(
        private val repository: TelegramRepository
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "telegram_art") return null
            return TelegramArtFetcher(options.context, data, repository)
        }
    }
}
