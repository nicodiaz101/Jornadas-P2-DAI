package ar.jornadas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ar.jornadas.lab.ServicioLocal

@Composable
fun Portada(
    state: ProgramaUiState,
    mesa: ServicioLocal,
    onActualizar: () -> Unit,
    onElegirDia: (Int) -> Unit,
    onElegirTema: (String) -> Unit,
    onElegirSeccion: (Seccion) -> Unit,
    onAbrirDetalle: (String) -> Unit,
    onCerrarDetalle: () -> Unit,
    onMarcar: (String) -> Unit
) {
    val listo = state as? ProgramaUiState.Listo
    BackHandler(listo != null && (listo.detalleId != null || listo.seccion != Seccion.PROGRAMA)) {
        if (listo?.detalleId != null) onCerrarDetalle() else onElegirSeccion(Seccion.PROGRAMA)
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF185B54), secondary = Color(0xFF995136))) {
        Scaffold(bottomBar = {
            if (listo != null && listo.detalleId == null) NavigationBar {
                Seccion.entries.forEach { seccion ->
                    NavigationBarItem(
                        selected = listo.seccion == seccion,
                        onClick = { onElegirSeccion(seccion) },
                        icon = { Text(seccion.icono) },
                        label = { Text(seccion.titulo) }
                    )
                }
            }
        }) { margen ->
            Column(
                Modifier.fillMaxSize().padding(margen).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text("Jornadas del Sur", style = MaterialTheme.typography.labelLarge)
                when (state) {
                    ProgramaUiState.Cargando -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    is ProgramaUiState.Listo -> {
                        if (state.detalleId != null) {
                            TextButton(onClick = onCerrarDetalle) { Text("Volver") }
                            val ficha = state.detalle
                            if (ficha != null) {
                                Column(
                                    Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(ficha.titulo, style = MaterialTheme.typography.headlineMedium)
                                    Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}")
                                    Text(ficha.ponente, style = MaterialTheme.typography.titleMedium)
                                    Text(ficha.resumen)
                                    Button(onClick = { onMarcar(ficha.id) }) {
                                        Text(if (ficha.favorita) "Quitar de mi agenda" else "Agregar a mi agenda")
                                    }
                                }
                            } else {
                                Text("Esta charla ya no forma parte del programa.")
                            }
                        } else {
                            Text(state.seccion.titulo, style = MaterialTheme.typography.headlineLarge)
                            when (state.seccion) {
                                Seccion.PROGRAMA -> Catalogo(
                                    state = state,
                                    onElegirDia = onElegirDia,
                                    onElegirTema = onElegirTema,
                                    onActualizar = onActualizar,
                                    onAbrir = onAbrirDetalle
                                )
                                Seccion.MI_AGENDA -> {
                                    if (state.avisoOffline) {
                                        Text(
                                            text = "Sin señal: se están usando datos guardados.",
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (state.favoritas.isEmpty()) {
                                        Text("Todavía no agregaste charlas.")
                                    }
                                    Grilla(state.favoritas, onAbrirDetalle)
                                }
                                Seccion.MESA -> Mesa(mesa)
                            }
                        }
                    }
                }
            }
        }
    }
}
