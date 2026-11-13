package ar.jornadas.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ar.jornadas.JornadasApp
import ar.jornadas.data.AgendaRepository
import ar.jornadas.data.Charla
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class Seccion(val titulo: String, val icono: String) {
    PROGRAMA("Programa", "▤"),
    MI_AGENDA("Mi agenda", "☆"),
    MESA("Mesa", "⚙")
}

sealed interface ProgramaUiState {
    data object Cargando : ProgramaUiState
    data class Listo(
        val visibles: List<Charla>,
        val favoritas: List<Charla>,
        val todas: List<Charla>,
        val dia: Int,
        val tema: String,
        val cargando: Boolean,
        val avisoOffline: Boolean,
        val sinDescarga: Boolean,
        val sinCharlas: Boolean,
        val seccion: Seccion,
        val detalleId: String?
    ) : ProgramaUiState {
        val detalle: Charla? get() = detalleId?.let { id -> todas.find { it.id == id } }
    }
}

class ProgramaModel(
    private val repo: AgendaRepository,
    private val savedState: SavedStateHandle,
    private val cargaAutomaticaHecha: AtomicBoolean
) : ViewModel() {

    private val _cargando = MutableStateFlow(false)
    private val _fallo = MutableStateFlow(false)

    private val dia = savedState.getStateFlow(KEY_DIA, 0)
    private val tema = savedState.getStateFlow(KEY_TEMA, TEMA_TODOS)
    private val seccion = savedState.getStateFlow(KEY_SECCION, Seccion.PROGRAMA.name)
    private val detalleId = savedState.getStateFlow<String?>(KEY_DETALLE, null)

    private data class DatosEstado(
        val charlas: List<Charla>,
        val hayEdicion: Boolean,
        val cargando: Boolean,
        val fallo: Boolean
    )

    private data class FiltrosEstado(
        val dia: Int,
        val tema: String,
        val seccion: Seccion,
        val detalleId: String?
    )

    private val datos = combine(repo.programa, repo.hayEdicion, _cargando, _fallo) {
            charlas, hayEdicion, cargando, fallo ->
        DatosEstado(charlas, hayEdicion, cargando, fallo)
    }

    private val filtros = combine(dia, tema, seccion, detalleId) { d, t, sec, det ->
        val seccionValida = runCatching { Seccion.valueOf(sec) }.getOrDefault(Seccion.PROGRAMA)
        FiltrosEstado(d, t, seccionValida, det)
    }

    val uiState: StateFlow<ProgramaUiState> = combine(datos, filtros) { d, f ->
        val visibles = d.charlas.filter {
            (f.dia == 0 || it.dia == f.dia) && (f.tema == TEMA_TODOS || it.tema == f.tema)
        }
        val favoritas = d.charlas.filter { it.favorita }
        ProgramaUiState.Listo(
            visibles = visibles,
            favoritas = favoritas,
            todas = d.charlas,
            dia = f.dia,
            tema = f.tema,
            cargando = d.cargando,
            avisoOffline = d.fallo && d.hayEdicion,
            sinDescarga = !d.hayEdicion && !d.cargando,
            sinCharlas = d.hayEdicion && d.charlas.isEmpty(),
            seccion = f.seccion,
            detalleId = f.detalleId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgramaUiState.Cargando
    )

    init {
        cargaAutomatica()
    }

    fun cargaAutomatica() {
        if (cargaAutomaticaHecha.compareAndSet(false, true)) {
            sincronizar()
        }
    }

    fun actualizar() {
        sincronizar()
    }

    private fun sincronizar() {
        if (_cargando.value) return
        _cargando.value = true
        _fallo.value = false
        viewModelScope.launch {
            try {
                repo.sincronizar()
                    .onSuccess { _fallo.value = false }
                    .onFailure { _fallo.value = true }
            } finally {
                _cargando.value = false
            }
        }
    }

    fun elegirDia(indice: Int) {
        savedState[KEY_DIA] = indice
    }

    fun elegirTema(nombre: String) {
        savedState[KEY_TEMA] = nombre
    }

    fun elegirSeccion(nueva: Seccion) {
        savedState[KEY_SECCION] = nueva.name
    }

    fun abrirDetalle(id: String) {
        savedState[KEY_DETALLE] = id
    }

    fun cerrarDetalle() {
        savedState[KEY_DETALLE] = null
    }

    fun marcar(id: String) {
        viewModelScope.launch {
            repo.cambiarFavorita(id)
        }
    }

    companion object {
        const val TEMA_TODOS = "Todos"
        val TEMAS = listOf(TEMA_TODOS, "Android", "Datos", "Calidad")
        val DIAS = listOf("Ambos días", "Día 1", "Día 2")

        private const val KEY_DIA = "dia"
        private const val KEY_TEMA = "tema"
        private const val KEY_SECCION = "seccion"
        private const val KEY_DETALLE = "detalle"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JornadasApp
                ProgramaModel(
                    repo = app.servicios.agenda,
                    savedState = createSavedStateHandle(),
                    cargaAutomaticaHecha = app.servicios.cargaAutomaticaHecha
                )
            }
        }
    }
}
