package com.example.cashcam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CashCamState(
    val draft: ProductDraft = ProductDraft(), val photo: String? = null,
    val cards: List<ProductCard> = emptyList(), val selected: Set<String> = emptySet(),
    val gallery: Boolean = false, val busy: Boolean = false, val loaded: Boolean = false,
    val message: String? = null
)

class CashCamViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    val repository = CardRepository(app)
    private val defaults = CardDefaults(app)
    private val mutable = MutableStateFlow(CashCamState(
        draft = ProductDraft(saved["title"] ?: "", saved["price"] ?: "", saved["status"] ?: "Nuevo",
            defaults.subtitle(), saved["properties"] ?: "", saved["ratio"] ?: 1f, defaults.brand()),
        photo = saved.get<String>("photo")?.takeIf { File(it).exists() }, gallery = saved["gallery"] ?: false
    ))
    val state = mutable.asStateFlow()

    fun load() {
        if (state.value.loaded || state.value.busy) return
        work { mutable.update { it.copy(cards = repository.list(), loaded = true) } }
    }
    fun edit(draft: ProductDraft) {
        if (draft.brand != state.value.draft.brand || draft.subtitle != state.value.draft.subtitle) {
            defaults.save(draft.brand, draft.subtitle)
        }
        saved["title"] = draft.title; saved["price"] = draft.price; saved["status"] = draft.status
        saved["subtitle"] = draft.subtitle; saved["properties"] = draft.properties; saved["ratio"] = draft.ratio
        mutable.update { it.copy(draft = draft) }
    }
    fun captured(file: File) {
        val previous = state.value.photo
        saved["photo"] = file.path
        mutable.update { it.copy(photo = file.path) }
        previous?.takeIf { it != file.path }?.let { File(it).delete() }
    }
    fun retake() {
        state.value.photo?.let { File(it).delete() }
        saved["photo"] = null
        mutable.update { it.copy(photo = null) }
    }
    fun gallery(show: Boolean) {
        if (state.value.busy) return
        saved["gallery"] = show
        mutable.update { it.copy(gallery = show, selected = emptySet()) }
    }
    fun select(id: String) = mutable.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }
    fun selectAll() = mutable.update { it.copy(selected = it.cards.map { card -> card.id }.toSet()) }
    fun clearSelection() = mutable.update { it.copy(selected = emptySet()) }
    fun message(text: String?) = mutable.update { it.copy(message = text) }
    fun create() {
        val snapshot = state.value
        val photo = snapshot.photo ?: return
        if (snapshot.draft.title.isBlank() || snapshot.draft.price.isBlank()) { message("Escribe el título y el precio"); return }
        work {
            val card = repository.create(File(photo), snapshot.draft)
            mutable.update { it.copy(cards = listOf(card) + it.cards, gallery = true, selected = setOf(card.id), message = "Ficha creada. Puedes guardarla o compartirla.") }
            saved["gallery"] = true
            retake()
        }
    }
    fun export(cards: List<ProductCard>) = work {
        val result = repository.saveToGallery(cards)
        message("${result.saved} guardadas · ${result.existing} ya estaban en galería" + if (result.failed > 0) " · ${result.failed} fallaron; vuelve a intentarlo" else "")
    }
    private fun work(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { message("No se pudo completar la operación. Revisa el espacio disponible e inténtalo de nuevo.") }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
