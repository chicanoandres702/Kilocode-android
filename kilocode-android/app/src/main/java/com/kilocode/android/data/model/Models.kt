package com.kilocode.android.data.model

data class Session(
    val id: String? = null,
    val directory: String? = null,
    val title: String? = null,
    val time: SessionTime? = null,
    val parentID: String? = null,
    val version: String? = null,
    val pinned: Boolean = false,
)

data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0,
)

data class Message(
    val id: String? = null,
    val sessionID: String? = null,
    val role: String? = null,
    val time: MessageTime? = null,
    val model: ModelInfo? = null,
    val agent: String? = null,
    val error: MessageError? = null,
    val parentID: String? = null,
    val modelID: String? = null,
    val providerID: String? = null,
    val mode: String? = null,
    val cost: Double = 0.0,
    val tokens: TokenUsage? = null,
)

data class MessageWithParts(
    val info: Message? = null,
    val parts: List<Part> = emptyList(),
)

data class MessageTime(
    val created: Long,
    val completed: Long? = null,
)

data class ModelOption(
    val providerID: String,
    @com.google.gson.annotations.SerializedName("id")
    val modelID: String,
    @com.google.gson.annotations.SerializedName("name")
    val displayName: String,
    val category: String? = "Models",
    val isFree: Boolean = false,
) {
    val key: String = "$providerID/$modelID"
}

data class ProviderListResponse(
    val all: List<Provider> = emptyList(),
    val connected: List<String> = emptyList(),
    val default: Map<String, String> = emptyMap(),
)

data class ModelInfo(
    val providerID: String,
    @com.google.gson.annotations.SerializedName(value = "modelID", alternate = ["id"])
    val modelID: String,
)

data class ProviderModel(
    val id: String,
    val name: String,
    val releaseDate: String? = null,
    val attachment: Boolean = false,
    val reasoning: Boolean = false,
    val temperature: Boolean = true,
    val toolCall: Boolean = true,
    val cost: ModelCost? = null,
    val limit: ModelLimit? = null,
)

data class SlashCommand(
    val command: String,
    val alias: String? = null,
    val label: String,
    val description: String,
    val category: String,
) {
    val trigger: String = if (command.startsWith("/")) command else "/$command"
}

data class TokenUsage(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: CacheUsage? = null,
)

data class CacheUsage(
    val read: Long = 0,
    val write: Long = 0,
)

data class MessageError(
    val name: String,
    val data: Map<String, Any>? = null,
)

data class Part(
    val id: String? = null,
    val sessionID: String? = null,
    val messageID: String? = null,
    val type: String? = null,
    val text: String? = null,
    val tool: String? = null,
    val state: ToolState? = null,
    val callID: String? = null,
    val synthetic: Boolean? = null,
    val ignored: Boolean? = null,
)

data class ToolState(
    val status: String,
    val input: Map<String, Any>? = null,
    val output: String? = null,
    val error: String? = null,
    val title: String? = null,
    val metadata: Map<String, Any>? = null,
    val time: ToolTime? = null,
)

data class ToolTime(
    val start: Long,
    val end: Long? = null,
)

data class Provider(
    val id: String,
    val name: String,
    val models: Map<String, Model> = emptyMap(),
)

data class Model(
    val id: String,
    val name: String,
    val releaseDate: String? = null,
    val attachment: Boolean = false,
    val reasoning: Boolean = false,
    val temperature: Boolean = true,
    val toolCall: Boolean = true,
    val cost: ModelCost? = null,
    val limit: ModelLimit? = null,
)

data class ModelCost(
    val input: Double = 0.0,
    val output: Double = 0.0,
    val cacheRead: Double = 0.0,
    val cacheWrite: Double = 0.0,
)

data class ModelLimit(
    val context: Long = 0,
    val output: Long = 0,
)

data class Project(
    val id: String,
    val name: String,
    val path: String,
)

data class FileNode(
    val name: String,
    val path: String,
    val absolute: String? = null,
    val type: String? = null,
    val ignored: Boolean = false,
) {
    val isDirectory: Boolean
        get() = type == "directory"
    val isFile: Boolean
        get() = type == "file"
}

data class Config(
    val providers: Map<String, ConfigProvider> = emptyMap(),
    val models: List<String> = emptyList(),
)

data class ConfigProvider(
    val id: String,
    val name: String,
    val apiKey: String? = null,
)

data class SessionStatus(
    val sessionID: String,
    val status: String,
)

data class McpServer(
    val name: String,
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
)

data class Agent(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val mode: String? = null,
    val builtIn: Boolean = false,
    val color: String? = null,
)

data class PromptRequest(
    val messageID: String? = null,
    val parts: List<PartRequest>? = null,
    val agent: String? = null,
    val model: ModelInfo? = null,
)

data class PartRequest(
    val type: String,
    val text: String,
)

data class CloneRepoRequest(
    val action: String,
    val repo: String,
)

data class RepoOperationResponse(
    val success: Boolean,
    val path: String? = null,
    val message: String? = null,
    val alreadyCloned: Boolean = false,
    val error: String? = null,
    val details: String? = null,
)

data class RepoEntry(
    val name: String,
    val path: String? = null,
    val modified: String? = null,
    val description: String? = null,
    val stars: Int = 0,
    val source: String = "local",
)

data class RepoListResponse(
    val repos: List<RepoEntry> = emptyList(),
    val source: String = "local",
)

// ── Planning models (milestones, issues) ─────────────────────────────────────

data class Milestone(
    val number: Int = 0,
    val title: String = "",
    val description: String? = null,
    @com.google.gson.annotations.SerializedName("state")
    val state: String = "open",
    @com.google.gson.annotations.SerializedName("open_issues")
    val openIssues: Int = 0,
    @com.google.gson.annotations.SerializedName("closed_issues")
    val closedIssues: Int = 0,
    @com.google.gson.annotations.SerializedName("html_url")
    val htmlUrl: String? = null,
    @com.google.gson.annotations.SerializedName("created_at")
    val createdAt: String? = null,
    @com.google.gson.annotations.SerializedName("updated_at")
    val updatedAt: String? = null,
    @com.google.gson.annotations.SerializedName("due_on")
    val dueOn: String? = null,
) {
    val totalIssues: Int get() = openIssues + closedIssues
    val isClosed: Boolean get() = state == "closed"
}

data class IssueLabel(
    val name: String = "",
    val color: String? = null,
    val description: String? = null,
)

data class Issue(
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    @com.google.gson.annotations.SerializedName("state")
    val state: String = "open",
    val labels: List<IssueLabel> = emptyList(),
    val milestone: Milestone? = null,
    @com.google.gson.annotations.SerializedName("html_url")
    val htmlUrl: String? = null,
    val assignee: String? = null,
    val comments: Int = 0,
    @com.google.gson.annotations.SerializedName("created_at")
    val createdAt: String? = null,
    @com.google.gson.annotations.SerializedName("updated_at")
    val updatedAt: String? = null,
) {
    val isClosed: Boolean get() = state == "closed"
}

data class MilestoneListResponse(
    val milestones: List<Milestone> = emptyList(),
    val totalCount: Int = 0,
)

data class IssueListResponse(
    val issues: List<Issue> = emptyList(),
    val totalCount: Int = 0,
)

// Request bodies
data class CreateMilestoneRequest(
    val type: String = "milestone",
    val title: String,
    val description: String? = null,
    val dueOn: String? = null,
)

data class CreateIssueRequest(
    val type: String = "issue",
    val title: String,
    val body: String? = null,
    val milestone: Int? = null,
    val labels: List<String> = emptyList(),
)

data class UpdateIssueStateRequest(
    val issueNumber: Int,
    val state: String,
)
