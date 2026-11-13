package ar.jornadas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.ui.Portada
import ar.jornadas.ui.ProgramaModel

class MainActivity : ComponentActivity() {
    private val model: ProgramaModel by viewModels { ProgramaModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val servicios = (application as JornadasApp).servicios
        if (savedInstanceState == null && intent.hasExtra("servicio_disponible")) {
            servicios.mesa.disponible = intent.getBooleanExtra("servicio_disponible", true)
            intent.removeExtra("servicio_disponible")
        }
        setContent {
            val state by model.uiState.collectAsStateWithLifecycle()
            Portada(
                state = state,
                mesa = servicios.mesa,
                onActualizar = model::actualizar,
                onElegirDia = model::elegirDia,
                onElegirTema = model::elegirTema,
                onElegirSeccion = model::elegirSeccion,
                onAbrirDetalle = model::abrirDetalle,
                onCerrarDetalle = model::cerrarDetalle,
                onMarcar = model::marcar
            )
        }
    }
}
