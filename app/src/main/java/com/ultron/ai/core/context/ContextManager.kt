package com.ultron.ai.core.context

import android.content.Context
import android.net.Uri
import com.ultron.ai.core.FileAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central Context Engine for ULTRON AI.
 * Coordinates document parsing, folder structure inspection, @ mentions resolution,
 * web extraction, Google Drive docs, and prepares synthesized context for the LLM.
 */
class ContextManager(
    private val context: Context,
    private val fileAnalyzer: FileAnalyzer = FileAnalyzer(context),
    private val folderAnalyzer: FolderAnalyzer = FolderAnalyzer(context),
    private val browserExtractor: BrowserExtractor = BrowserExtractor(),
    private val driveManager: GoogleDriveManager = GoogleDriveManager(context),
    private val actionExecutor: ContextActionExecutor = ContextActionExecutor(context)
) {

    companion object {
        const val MAX_COMBINED_CONTEXT_CHARS = 16_000
    }

    /**
     * Reads a document or image chosen via Storage Access Framework (SAF).
     */
    suspend fun processFile(uri: Uri): ContextItem.FileAttachment = withContext(Dispatchers.IO) {
        val outcome = fileAnalyzer.read(uri)
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        when (outcome) {
            is FileAnalyzer.Outcome.Text -> {
                ContextItem.FileAttachment(
                    id = "file-${System.currentTimeMillis()}",
                    displayName = outcome.name,
                    uri = uri,
                    mimeType = mime,
                    sizeBytes = outcome.text.length.toLong(),
                    extractedText = outcome.text,
                    isImage = false
                )
            }
            is FileAnalyzer.Outcome.Image -> {
                ContextItem.FileAttachment(
                    id = "img-${System.currentTimeMillis()}",
                    displayName = outcome.name,
                    uri = uri,
                    mimeType = "image/jpeg",
                    sizeBytes = 0L,
                    extractedText = "[Image: ${outcome.name} attached for visual analysis]",
                    isImage = true,
                    imageBase64 = outcome.jpegBase64
                )
            }
            is FileAnalyzer.Outcome.Unsupported -> {
                ContextItem.FileAttachment(
                    id = "unsupported-${System.currentTimeMillis()}",
                    displayName = outcome.name,
                    uri = uri,
                    mimeType = outcome.mimeType,
                    sizeBytes = 0L,
                    extractedText = "[Unsupported file format: ${outcome.mimeType}]"
                )
            }
            is FileAnalyzer.Outcome.TooLarge -> {
                ContextItem.FileAttachment(
                    id = "toolarge-${System.currentTimeMillis()}",
                    displayName = outcome.name,
                    uri = uri,
                    mimeType = mime,
                    sizeBytes = outcome.limitBytes,
                    extractedText = "[File exceeded maximum size limit: ${outcome.limitBytes} bytes]"
                )
            }
            is FileAnalyzer.Outcome.Unreadable -> {
                ContextItem.FileAttachment(
                    id = "unreadable-${System.currentTimeMillis()}",
                    displayName = outcome.name,
                    uri = uri,
                    mimeType = mime,
                    sizeBytes = 0L,
                    extractedText = "[Unable to read file content]"
                )
            }
        }
    }

    /**
     * Inspects a directory tree picked via ACTION_OPEN_DOCUMENT_TREE.
     */
    suspend fun processFolder(treeUri: Uri): ContextItem.FolderContext = withContext(Dispatchers.IO) {
        folderAnalyzer.analyzeFolder(treeUri)
    }

    /**
     * Resolves an intelligent @ mention.
     */
    fun resolveMention(category: MentionCategory): ContextItem.MentionReference {
        val snippet = when (category) {
            MentionCategory.FILES -> "Active attachments, documents, and spreadsheets."
            MentionCategory.FOLDER -> "Project tree, Gradle configurations, and Android dependencies."
            MentionCategory.PROJECTS -> "Application module structure and architecture."
            MentionCategory.WEBSITE -> "Live web retrieval and webpage analyzer."
            MentionCategory.DRIVE -> "Google Drive documents and spreadsheets."
            MentionCategory.MEMORY -> "Neural long-term facts and conversation preferences."
        }
        return ContextItem.MentionReference(
            id = "mention-${category.tag}",
            displayName = category.tag,
            tag = category.tag,
            resolvedSnippet = snippet
        )
    }

    /**
     * Extracts content from a URL via BrowserExtractor.
     */
    suspend fun processWebUrl(url: String): ContextItem.WebContext = withContext(Dispatchers.IO) {
        browserExtractor.extractWebPage(url)
    }

    /**
     * Lists documents from Google Drive.
     */
    suspend fun getDriveDocuments(): List<ContextItem.DriveContext> = withContext(Dispatchers.IO) {
        driveManager.getAuthorizedDocuments()
    }

    /**
     * Available predefined actions.
     */
    fun getAvailableActions(): List<ContextItem.ActionRequestItem> = actionExecutor.getAvailableActions()

    /**
     * Executes a confirmed action.
     */
    suspend fun executeAction(action: ContextItem.ActionRequestItem): Boolean =
        actionExecutor.executeAction(action)

    /**
     * Bundles user prompt and attached context items into a coherent, chunked AI prompt.
     */
    fun buildAugmentedPrompt(userMessage: String, contextItems: List<ContextItem>): String {
        if (contextItems.isEmpty()) return userMessage

        val contextBuilder = StringBuilder()
        contextBuilder.appendLine("The user has provided the following verified context items:")
        contextBuilder.appendLine()

        for (item in contextItems) {
            when (item) {
                is ContextItem.FileAttachment -> {
                    contextBuilder.appendLine("--- ATTACHED FILE: ${item.displayName} (${item.mimeType}) ---")
                    contextBuilder.appendLine(item.extractedText.take(4_000))
                    contextBuilder.appendLine()
                }
                is ContextItem.FolderContext -> {
                    contextBuilder.appendLine("--- ATTACHED FOLDER: ${item.displayName} (${item.fileCount} files) ---")
                    contextBuilder.appendLine(item.structureSummary.take(6_000))
                    contextBuilder.appendLine()
                }
                is ContextItem.MentionReference -> {
                    contextBuilder.appendLine("--- CONTEXT SOURCE: ${item.tag} ---")
                    contextBuilder.appendLine(item.resolvedSnippet)
                    contextBuilder.appendLine()
                }
                is ContextItem.WebContext -> {
                    contextBuilder.appendLine("--- WEBPAGE EXTRACT: ${item.title} (${item.url}) ---")
                    contextBuilder.appendLine(item.contentSummary.take(4_000))
                    contextBuilder.appendLine()
                }
                is ContextItem.DriveContext -> {
                    contextBuilder.appendLine("--- GOOGLE DRIVE DOCUMENT: ${item.displayName} ---")
                    contextBuilder.appendLine(item.textContent.take(4_000))
                    contextBuilder.appendLine()
                }
                is ContextItem.ActionRequestItem -> {
                    contextBuilder.appendLine("--- ACTION EXECUTION STATUS: ${item.displayName} [${item.status}] ---")
                    contextBuilder.appendLine("Action: ${item.actionType}, Target: ${item.target}")
                    contextBuilder.appendLine()
                }
            }
        }

        contextBuilder.appendLine("--- USER QUERY ---")
        contextBuilder.appendLine(userMessage)

        return contextBuilder.toString().take(MAX_COMBINED_CONTEXT_CHARS)
    }
}
