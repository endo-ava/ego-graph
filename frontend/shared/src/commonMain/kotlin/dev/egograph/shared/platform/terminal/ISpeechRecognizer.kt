package dev.egograph.shared.platform.terminal

import kotlinx.coroutines.flow.Flow

/**
 * 音声認識インターフェース
 *
 * プラットフォーム固有の音声認識機能を抽象化する。
 */
interface ISpeechRecognizer {
    /**
     * 音声認識を開始し、認識結果をFlowとして返す
     *
     * @return 認識されたテキストのストリーム。リアルタイムで更新される。
     */
    suspend fun startRecognition(): Flow<String>

    /**
     * 音声認識を停止する
     */
    fun stopRecognition()
}
