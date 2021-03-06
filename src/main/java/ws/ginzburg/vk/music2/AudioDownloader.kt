package ws.ginzburg.vk.music2

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels

/**
 * Created by Ginzburg on 04/06/2017.
 */
class AudioDownloader(val audios: List<Audio>, val directory: File, val overwriteExisting: Boolean, val progressReporter: (Int) -> Unit, val finishReporter: () -> Unit, val failReporter: (Audio, Throwable) -> ErrorResponse) {
    private @Volatile var isCanceled = false

    fun start() {
        Thread({
            for ((i, audio) in audios.withIndex()) {
                var stepFinished = false
                while (!stepFinished) {
                    if (isCanceled) break
                    try {
                        val fileName = (audios.size - i).toString() + " - " + normalizeFileName(audio.title) + " - " + normalizeFileName(audio.artist) + ".mp3"
                        val file = File(directory, fileName)
                        if (overwriteExisting || !file.exists()) {
                            val audioURL = URL(audio.url)
                            val channel = Channels.newChannel(audioURL.openStream())
                            val outputStream = FileOutputStream(file)

                            val debugThrowException = false
                            if (debugThrowException) throw IOException("Debug exception")

                            outputStream.channel.transferFrom(channel, 0, Long.MAX_VALUE)
                            outputStream.close()
                        }
                        stepFinished = true
                    } catch (e: Throwable) {
                        val action = failReporter(audio, e)
                        when (action) {
                            ErrorResponse.CANCEL -> {
                                cancel()
                                stepFinished = true
                            }
                            ErrorResponse.RETRY -> {
                                stepFinished = false
                            }
                            ErrorResponse.SKIP -> {
                                stepFinished = true
                            }
                        }
                    }
                }
                progressReporter(i + 1)
            }
            finishReporter()
        }).start()
    }

    fun cancel() {
        isCanceled = true
    }

    private fun normalizeFileName(fileName: String):String {
        val shortened =
                if (fileName.length > 100)
                    fileName.substring(0, 97) + "..."
                else
                    fileName
        return shortened.replace(Regex("[^\\pL0-9.-]"), "_")
    }


}