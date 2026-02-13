package dev.egograph.shared.core.platform.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * モック用音声認識実装
 *
 * テストやプレビュー用のダミー実装。
 */
class MockSpeechRecognizer : ISpeechRecognizer {
    override suspend fun startRecognition(): Flow<String> =
        flowOf(
            "This is a mock speech recognition result",
            "You can edit this text before sending",
            "Voice input is now available",
        )

    override fun stopRecognition() {
        // No-op for mock
    }
}

/**
 * モック音声認識を作成する
 *
 * @return モックISpeechRecognizer
 */
fun createMockSpeechRecognizer(): ISpeechRecognizer = MockSpeechRecognizer()
