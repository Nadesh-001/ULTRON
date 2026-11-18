package com.ultron.ai.core.context

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Analyzes directories picked via ACTION_OPEN_DOCUMENT_TREE.
 * Specifically inspects Android/Kotlin project structures, Gradle build scripts,
 * and manifest files for architecture understanding and build problem detection.
 */
class FolderAnalyzer(private val context: Context) {

    companion object {
        private const val MAX_TREE_FILES = 120
        private const val MAX_FILE_READ_BYTES = 64 * 1024 // 64 KB per key file
    }

    /**
     * Traverses the folder tree represented by [treeUri], gathers file paths,
     * reads high-value build/configuration files, and produces a structured summary.
     */
    suspend fun analyzeFolder(treeUri: Uri): ContextItem.FolderContext = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val folderName = getTreeDisplayName(resolver, treeUri) ?: "Project-Root"

        val treeDocumentUri = if (DocumentsContract.isDocumentUri(context, treeUri)) {
            treeUri
        } else {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        }

        val allFiles = mutableListOf<String>()
        val keyFilesContent = mutableMapOf<String, String>()

        traverseDirectory(
            resolver = resolver,
            treeUri = treeUri,
            parentDocId = DocumentsContract.getDocumentId(treeDocumentUri),
            currentPath = folderName,
            allFiles = allFiles,
            keyFilesContent = keyFilesContent
        )

        // Identify potential build problems or dependency conflicts in Gradle files
        val analysisInsights = auditBuildScripts(keyFilesContent)

        val summaryBuilder = StringBuilder().apply {
            appendLine("=== Project Folder Context: $folderName ===")
            appendLine("Total Files Scanned: ${allFiles.size}")
            appendLine()
            appendLine("Structure Overview:")
            allFiles.take(40).forEach { appendLine("  - $it") }
            if (allFiles.size > 40) {
                appendLine("  ... (${allFiles.size - 40} more files)")
            }
            appendLine()
            if (analysisInsights.isNotBlank()) {
                appendLine("Build & Architecture Insights:")
                appendLine(analysisInsights)
            }
        }

        ContextItem.FolderContext(
            id = "folder-${System.currentTimeMillis()}",
            displayName = folderName,
            treeUri = treeUri,
            fileCount = allFiles.size,
            structureSummary = summaryBuilder.toString(),
            keyFilesContent = keyFilesContent
        )
    }

    private fun traverseDirectory(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        currentPath: String,
        allFiles: MutableList<String>,
        keyFilesContent: MutableMap<String, String>,
        depth: Int = 0
    ) {
        if (depth > 5 || allFiles.size >= MAX_TREE_FILES) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        val cursor: Cursor? = try {
            resolver.query(childrenUri, projection, null, null, null)
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (it.moveToNext() && allFiles.size < MAX_TREE_FILES) {
                val docId = it.getString(idCol)
                val name = it.getString(nameCol) ?: continue
                val mime = it.getString(mimeCol) ?: ""
                val fullPath = "$currentPath/$name"

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    // Skip hidden / build output directories
                    if (!name.startsWith(".") && name != "build" && name != ".gradle") {
                        traverseDirectory(resolver, treeUri, docId, fullPath, allFiles, keyFilesContent, depth + 1)
                    }
                } else {
                    allFiles.add(fullPath)
                    // If this is a key project/build file, read its text
                    if (isKeyFile(name)) {
                        readDocumentContent(resolver, treeUri, docId)?.let { content ->
                            keyFilesContent[name] = content
                        }
                    }
                }
            }
        }
    }

    private fun isKeyFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "build.gradle.kts" ||
                lower == "build.gradle" ||
                lower == "settings.gradle.kts" ||
                lower == "settings.gradle" ||
                lower == "androidmanifest.xml" ||
                lower == "gradle.properties" ||
                lower == "pom.xml" ||
                lower == "package.json"
    }

    private fun readDocumentContent(resolver: ContentResolver, treeUri: Uri, docId: String): String? {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            resolver.openInputStream(docUri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    val sb = StringBuilder()
                    var bytesRead = 0
                    var line: String? = reader.readLine()
                    while (line != null && bytesRead < MAX_FILE_READ_BYTES) {
                        sb.appendLine(line)
                        bytesRead += line.length + 1
                        line = reader.readLine()
                    }
                    sb.toString()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun auditBuildScripts(keyFiles: Map<String, String>): String {
        val insights = StringBuilder()
        val gradleContent = keyFiles["build.gradle.kts"] ?: keyFiles["build.gradle"] ?: ""

        if (gradleContent.isNotBlank()) {
            if (gradleContent.contains("composeBom") || gradleContent.contains("androidx.compose:compose-bom")) {
                insights.appendLine("• Jetpack Compose BOM detected. Ensuring unified Compose library alignment.")
            }
            if (gradleContent.contains("ksp") || gradleContent.contains("kapt")) {
                insights.appendLine("• Annotation processor (KSP/KAPT) detected for local code generation.")
            }
            if (gradleContent.contains("koin")) {
                insights.appendLine("• Koin dependency injection framework configured.")
            }
            if (gradleContent.contains("targetSdk = 35") || gradleContent.contains("targetSdkVersion 35")) {
                insights.appendLine("• Configured for Android 15 (API 35). Edge-to-edge enforcement enabled.")
            }
        }

        return insights.toString()
    }

    private fun getTreeDisplayName(resolver: ContentResolver, treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return try {
            resolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
