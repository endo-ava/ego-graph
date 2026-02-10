package dev.egograph.shared.platform.terminal

/**
 * プラットフォーム固有のSpeechRecognizerを作成する
 *
 * @return プラットフォーム固有のSpeechRecognizer実装
 */
expect fun createSpeechRecognizer(): ISpeechRecognizer
