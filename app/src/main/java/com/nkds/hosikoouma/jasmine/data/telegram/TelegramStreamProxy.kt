package com.nkds.hosikoouma.jasmine.data.telegram

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.header
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.writeFully
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.RandomAccessFile
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramStreamProxy @Inject constructor(
    private val telegramRepository: TelegramRepository
) {
    private var server: ApplicationEngine? = null
    private val proxyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startJob: Job? = null
    @Volatile private var actualPort: Int = 0

    private companion object {
        private const val TAG = "TelegramStreamProxy"
    }

    private fun createServer(port: Int): ApplicationEngine {
        return embeddedServer(
            CIO,
            host = "127.0.0.1",
            port = port,
            configure = {
                reuseAddress = true
            }
        ) {
            routing {
                get("/stream/{fileId}") {
                    val fileId = call.parameters["fileId"]?.toIntOrNull()
                    if (fileId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid File ID")
                        return@get
                    }

                    Log.d(TAG, "Incoming request for fileId: $fileId")
                    
                    // Проверяем статус TDLib
                    if (!telegramRepository.isReady()) {
                        telegramRepository.awaitReady(5000L)
                    }
                    
                    // Сначала просто проверяем файл, не запуская загрузку (важно для оффлайна)
                    var fileInfo = telegramRepository.getFile(fileId)
                    
                    // Если файла нет или он не скачан, пробуем инициировать загрузку (только если есть сеть)
                    if (fileInfo?.local?.isDownloadingCompleted == false) {
                        Log.d(TAG, "File not complete, requesting download: $fileId")
                        fileInfo = telegramRepository.downloadFile(fileId, 1)
                    }
                    
                    // Ждем появления пути (если файл только начал качаться)
                    var pathWaitCount = 0
                    while (fileInfo?.local?.path.isNullOrEmpty() && pathWaitCount < 40) {
                        delay(100)
                        fileInfo = telegramRepository.getFile(fileId)
                        pathWaitCount++
                    }

                    val path = fileInfo?.local?.path
                    if (path.isNullOrEmpty()) {
                        Log.e(TAG, "Could not resolve path for fileId: $fileId")
                        call.respond(HttpStatusCode.NotFound, "File path not available")
                        return@get
                    }
                    
                    val file = File(path)
                    var expectedSize = fileInfo?.expectedSize ?: 0L
                    if (expectedSize <= 0L && file.exists()) expectedSize = file.length()

                    // Обработка Range запросов (перемотка)
                    val rangeValidation = CloudStreamSecurity.validateRangeHeader(call.request.headers["Range"])
                    val isRangeRequest = rangeValidation.normalizedHeader != null
                    var start = rangeValidation.startInclusive ?: 0L
                    var end = rangeValidation.endInclusive ?: if (expectedSize > 0) expectedSize - 1 else Long.MAX_VALUE - 1

                    if (expectedSize > 0 && end >= expectedSize) end = expectedSize - 1
                    
                    val contentLength = if (expectedSize > 0) (end - start + 1) else -1L

                    call.response.header("Accept-Ranges", "bytes")
                    if (isRangeRequest && expectedSize > 0) {
                        call.response.header("Content-Range", "bytes $start-$end/$expectedSize")
                        call.response.status(HttpStatusCode.PartialContent)
                    }
                    if (contentLength > 0) {
                        call.response.header("Content-Length", contentLength.toString())
                    }

                    call.respondBytesWriter(contentType = ContentType.Audio.Any) {
                        if (!file.exists()) {
                            Log.e(TAG, "File does not exist on disk: $path")
                            return@respondBytesWriter
                        }

                        val raf = RandomAccessFile(file, "r")
                        try {
                            var currentPos = start
                            val buffer = ByteArray(64 * 1024)
                            raf.seek(currentPos)
                            
                            while (currentPos <= end) {
                                // Проверяем, сколько байт реально доступно в файле сейчас
                                // Это критично для стриминга во время загрузки
                                val currentFileInfo = telegramRepository.getFile(fileId)
                                val availableSize = currentFileInfo?.local?.downloadedPrefixSize?.toLong() ?: file.length()
                                
                                if (currentPos < availableSize) {
                                    val toRead = min(buffer.size.toLong(), (availableSize - currentPos)).toInt()
                                    val read = raf.read(buffer, 0, toRead)
                                    if (read > 0) {
                                        writeFully(buffer, 0, read)
                                        currentPos += read
                                    }
                                } else {
                                    // Мы дошли до конца того, что скачано
                                    if (currentFileInfo?.local?.isDownloadingCompleted == true) break
                                    
                                    // Ждем новых байтов
                                    delay(200)
                                    if (!file.exists()) break 
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Stream interrupted: ${e.message}")
                        } finally {
                            raf.close()
                        }
                    }
                }
            }
        }
    }

    fun start() {
        if (actualPort != 0) return
        startJob = proxyScope.launch {
            try {
                val freePort = ServerSocket(0).use { it.localPort }
                val createdServer = createServer(freePort)
                createdServer.start(wait = false)
                actualPort = freePort
                Log.d(TAG, "Proxy started on port $actualPort")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start proxy server", e)
            }
        }
    }

    fun stop() {
        startJob?.cancel()
        proxyScope.coroutineContext.cancelChildren()
        server?.stop(500, 1000)
        server = null
        actualPort = 0
    }
    
    suspend fun getProxyUrl(fileId: Int): String {
        if (actualPort == 0) {
            start()
            withTimeoutOrNull(3000) {
                while (actualPort == 0) delay(50)
            }
        }
        return "http://127.0.0.1:$actualPort/stream/$fileId"
    }
}
