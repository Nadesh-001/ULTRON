package com.ultron.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultron.ai.core.links.QuickLink
import com.ultron.ai.core.links.QuickLinks
import com.ultron.ai.service.links.QuickLinkStore
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Settings → text text: the owner's phrases and the sites they open. */
class QuickLinksViewModel(private val store: QuickLinkStore) : ViewModel() {

    val links: StateFlow<List<QuickLink>> = store.links

    init {
        // A settings import may have brought new ones.
        viewModelScope.launch { store.reload() }
    }

    /**
     * Saves what was typed; the reason it can't be saved otherwise.
     * [id] null adds a new link.
     */
    fun save(id: String?, name: String, phrases: String, address: String, silent: Boolean): String? {
        val said = QuickLinks.phrasesOf(phrases)
        val url = QuickLinks.webAddress(address)
        return when {
            said.isEmpty() -> "Add at least one trigger phrase"
            url == null -> "Please enter a valid website URL"
            else -> {
                val title = name.trim().ifEmpty { said.first() }
                val link = QuickLink(id ?: UUID.randomUUID().toString(), title, said, url, silent)
                viewModelScope.launch { store.save(link) }
                null
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }
}
