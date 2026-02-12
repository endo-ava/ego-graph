package dev.egograph.shared.core.platform.terminal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.ERROR_AUDIO
import android.speech.SpeechRecognizer.ERROR_CLIENT
import android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
import android.speech.SpeechRecognizer.ERROR_NETWORK
import android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT
import android.speech.SpeechRecognizer.ERROR_NO_MATCH
import android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY
import android.speech.SpeechRecognizer.ERROR_SERVER
import android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android音声認識実装
 *
 * AndroidのSpeechRecognizer APIを使用して音声認識を行う。
 * RECORD_AUDIOパーミッションが必要。
 */
actual fun createSpeechRecognizer(): ISpeechRecognizer = AndroidSpeechRecognizer()

/**
 * Android音声認識の実装クラス
 */
class AndroidSpeechRecognizer : ISpeechRecognizer {
    private var speechRecognizer: SpeechRecognizer? = null

    override suspend fun startRecognition(): Flow<String> =
        callbackFlow {
            val context =
                ActivityRecorder.currentActivity
                    ?: throw IllegalStateException("No active context available")

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                }

            val listener =
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        // 音声認識の準備完了
                    }

                    override fun onBeginningOfSpeech() {
                        // 音声入力開始
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // 音量レベル変化（必要に応じてUI表示に使用）
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // 音声バッファ受信
                    }

                    override fun onEndOfSpeech() {
                        // 音声入力終了
                    }

                    override fun onError(error: Int) {
                        val errorMessage =
                            when (error) {
                                ERROR_AUDIO -> "Audio recording error"
                                ERROR_CLIENT -> "Client error"
                                ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                                ERROR_NETWORK -> "Network error"
                                ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                ERROR_NO_MATCH -> "No match found"
                                ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                                ERROR_SERVER -> "Server error"
                                ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                else -> "Unknown error"
                            }
                        close(RuntimeException("Speech recognition error: $errorMessage"))
                    }

                    override fun onResults(results: Bundle?) {
                        val matches =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            trySend(matches[0])
                        }
                        // 最終結果が得られたらストリームを閉じる
                        close()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            trySend(matches[0])
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) {
                        // イベント処理
                    }
                }

            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)

            awaitClose {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }

    override fun stopRecognition() {
        speechRecognizer?.stopListening()
    }
}

/**
 * 現在のアクティビティを保持するオブジェクト
 *
 * Android側でMainActivityから設定する必要がある。
 */
object ActivityRecorder {
    var currentActivity: Context? = null
}
