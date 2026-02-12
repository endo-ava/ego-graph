package dev.egograph.shared.core.domain.repository

import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.domain.model.SystemPromptResponse

interface SystemPromptRepository {
    suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse>

    suspend fun updateSystemPrompt(
        name: SystemPromptName,
        content: String,
    ): RepositoryResult<SystemPromptResponse>
}
