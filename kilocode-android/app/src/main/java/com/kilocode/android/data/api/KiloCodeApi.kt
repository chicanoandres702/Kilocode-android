package com.kilocode.android.data.api

import com.google.gson.JsonObject
import com.kilocode.android.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KiloCodeApi {

    // Session endpoints
    @GET("session")
    suspend fun listSessions(
        @Query("directory") directory: String? = null,
        @Query("roots") roots: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
    ): Response<List<Session>>

    @GET("session/{sessionID}")
    suspend fun getSession(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
    ): Response<Session>

    @POST("session")
    suspend fun createSession(
        @Query("directory") directory: String? = null,
        @Query("workspace") workspace: String? = null,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<Session>

    @DELETE("session/{sessionID}")
    suspend fun deleteSession(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
    ): Response<Unit>

    @GET("session/{sessionID}/message")
    suspend fun listMessages(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("order") order: String? = null,
        @Query("cursor") cursor: String? = null,
    ): Response<List<MessageWithParts>>

    @GET("session/{sessionID}/message/{messageID}")
    suspend fun getMessage(
        @Path("sessionID") sessionID: String,
        @Path("messageID") messageID: String,
    ): Response<MessageWithParts>

    @POST("session/{sessionID}/prompt_async")
    suspend fun sendPrompt(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
        @Body request: PromptRequest,
    ): Response<Unit>

    @POST("session/{sessionID}/message")
    suspend fun sendMessage(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
        @Body request: PromptRequest,
    ): Response<MessageWithParts>

    @POST("session/{sessionID}/abort")
    suspend fun abortSession(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
    ): Response<Unit>

    @POST("session/{sessionID}/compact")
    suspend fun compactSession(
        @Path("sessionID") sessionID: String,
        @Query("directory") directory: String? = null,
    ): Response<JsonObject>

    @GET("session/status")
    suspend fun getSessionStatus(): Response<Map<String, SessionStatus>>

    // File endpoints
    @GET("file")
    suspend fun listFiles(
        @Query("path") path: String? = null,
    ): Response<List<FileNode>>

    @GET("file/read")
    suspend fun readFile(
        @Query("path") path: String,
    ): Response<Map<String, String>>

    // Provider endpoints
    @GET("provider")
    suspend fun listProviders(): Response<ProviderListResponse>

    @GET("api/model")
    suspend fun listModels(): Response<List<ModelOption>>

    // Agent endpoints
    @GET("agent")
    suspend fun listAgents(): Response<List<Agent>>

    // Config endpoints
    @GET("config")
    suspend fun getConfig(): Response<Config>

    // Project endpoints
    @GET("project")
    suspend fun getProject(): Response<Project>

    // MCP endpoints
    @GET("mcp")
    suspend fun listMcpServers(): Response<List<McpServer>>

    @POST("mcp")
    suspend fun addMcpServer(
        @Body server: McpServer,
    ): Response<McpServer>

    @POST("api/auth/github")
    suspend fun authenticateGitHub(@Body body: Map<String, String>): Response<Unit>

    // Repo endpoints
    @POST("api/repo")
    suspend fun repoOperation(@Body request: CloneRepoRequest): Response<RepoOperationResponse>

    @GET("api/repo")
    suspend fun listRepos(): Response<RepoListResponse>

    @GET("api/repo/search")
    suspend fun searchRepos(@Query("q") query: String): Response<RepoListResponse>

    @DELETE("mcp/{name}")
    suspend fun removeMcpServer(
        @Path("name") name: String,
    ): Response<Unit>

    // ── Planning endpoints ─────────────────────────────────────────────────────

    @GET("api/planning/milestones")
    suspend fun listMilestones(
        @Query("state") state: String? = null,
    ): Response<MilestoneListResponse>

    @GET("api/planning/issues")
    suspend fun listMilestoneIssues(
        @Query("milestone") milestoneNumber: Int,
        @Query("state") state: String? = null,
    ): Response<IssueListResponse>

    @POST("api/planning")
    suspend fun createMilestone(
        @Body request: CreateMilestoneRequest,
    ): Response<Milestone>

    @POST("api/planning")
    suspend fun createIssue(
        @Body request: CreateIssueRequest,
    ): Response<Issue>

    @PATCH("api/planning")
    suspend fun updateIssueState(
        @Body request: UpdateIssueStateRequest,
    ): Response<Issue>
}
