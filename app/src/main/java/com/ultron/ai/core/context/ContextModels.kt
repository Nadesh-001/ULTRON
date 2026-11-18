package com.ultron.ai.core.context

import android.net.Uri

/**
 * Universal Context Items supported by the ULTRON AI Context Engine.
 */
sealed interface ContextItem {
    val id: String
    val displayName: String
    val typeLabel: String

    /** Selected document, spreadsheet, image or text file. */
    data class FileAttachment(
        override val id: String,
        override val displayName: String,
        val uri: Uri,
        val mimeType: String,
        val sizeBytes: Long,
        val extractedText: String,
        val isImage: Boolean = false,
        val imageBase64: String? = null
    ) : ContextItem {
        override val typeLabel = "FILE"
    }

    /** Folder / project tree selected via ACTION_OPEN_DOCUMENT_TREE. */
    data class FolderContext(
        override val id: String,
        override val displayName: String,
        val treeUri: Uri,
        val fileCount: Int,
        val structureSummary: String,
        val keyFilesContent: Map<String, String> = emptyMap()
    ) : ContextItem {
        override val typeLabel = "FOLDER"
    }

    /** Smart context mention (@Files, @Folder, @Projects, @Website, @Drive, @Memory). */
    data class MentionReference(
        override val id: String,
        override val displayName: String,
        val tag: String,
        val resolvedSnippet: String
    ) : ContextItem {
        override val typeLabel = "MENTION"
    }

    /** Webpage extract / public web content. */
    data class WebContext(
        override val id: String,
        override val displayName: String,
        val url: String,
        val title: String,
        val contentSummary: String
    ) : ContextItem {
        override val typeLabel = "BROWSER"
    }

    /** Google Drive document accessed via OAuth. */
    data class DriveContext(
        override val id: String,
        override val displayName: String,
        val fileId: String,
        val mimeType: String,
        val textContent: String
    ) : ContextItem {
        override val typeLabel = "DRIVE"
    }

    /** Predefined action / routine execution request requiring user confirmation. */
    data class ActionRequestItem(
        override val id: String,
        override val displayName: String,
        val actionType: String,
        val target: String,
        val parameters: Map<String, String> = emptyMap(),
        var status: ActionStatus = ActionStatus.PENDING
    ) : ContextItem {
        override val typeLabel = "ACTION"
    }
}

enum class ActionStatus {
    PENDING,
    CONFIRMED,
    EXECUTED,
    CANCELLED,
    FAILED
}

/** Predefined mention categories supported in the @ autocomplete bar. */
enum class MentionCategory(val tag: String, val description: String) {
    FILES("@Files", "Active and uploaded document attachments"),
    FOLDER("@Folder", "Project hierarchy, Gradle build scripts, and Kotlin files"),
    PROJECTS("@Projects", "Workspace project structure and architecture"),
    WEBSITE("@Website", "Live web search & URL content extractor"),
    DRIVE("@Drive", "Google Drive documents and spreadsheets"),
    MEMORY("@Memory", "Long-term neural memories, notes, and preferences")
}
