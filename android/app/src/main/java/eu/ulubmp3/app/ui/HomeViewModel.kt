package eu.ulubmp3.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ulubmp3.app.data.Category
import eu.ulubmp3.app.data.Track
import eu.ulubmp3.app.data.UlubApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val categories: List<Category> = emptyList(),
    val top: List<Track> = emptyList(),
    val latest: List<Track> = emptyList(),
    val selectedCategory: Category? = null,
    val playing: Track? = null,
    val error: String? = null
)

class HomeViewModel(
    private val api: UlubApiClient = UlubApiClient()
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { api.home() }
                .onSuccess {
                    _state.value = _state.value.copy(
                        loading = false,
                        categories = it.categories,
                        top = it.top,
                        latest = it.latest
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Nie udało się pobrać danych: ${error.message ?: "błąd połączenia"}"
                    )
                }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value, selectedCategory = null, error = null)
        searchJob?.cancel()
        if (value.isBlank()) {
            refresh()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runCatching { api.search(value) }
                .onSuccess { _state.value = _state.value.copy(latest = it, loading = false, error = null) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        latest = emptyList(),
                        error = "Błąd wyszukiwania: ${error.message ?: "błąd połączenia"}"
                    )
                }
        }
    }

    fun selectCategory(category: Category?) {
        _state.value = _state.value.copy(selectedCategory = category, query = "", error = null)
        if (category == null) {
            refresh()
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { api.category(category.slug) }
                .onSuccess { _state.value = _state.value.copy(latest = it, loading = false, error = null) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        latest = emptyList(),
                        error = "Nie udało się wczytać kategorii: ${error.message ?: "błąd połączenia"}"
                    )
                }
        }
    }

    fun play(track: Track) {
        _state.value = _state.value.copy(playing = track)
    }

    fun stop() {
        _state.value = _state.value.copy(playing = null)
    }
}
