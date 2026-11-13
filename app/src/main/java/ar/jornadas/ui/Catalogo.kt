package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.jornadas.data.Charla

@Composable
fun Catalogo(
    state: ProgramaUiState.Listo,
    onElegirDia: (Int) -> Unit,
    onElegirTema: (String) -> Unit,
    onActualizar: () -> Unit,
    onAbrir: (String) -> Unit
) {
    val fichas = if (state.sinDescarga || state.sinCharlas) emptyList() else state.visibles
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item(key = "filtros_dia") {
            Selector(ProgramaModel.DIAS, state.dia, onElegirDia)
        }
        item(key = "filtros_tema") {
            Selector(ProgramaModel.TEMAS, ProgramaModel.TEMAS.indexOf(state.tema).coerceAtLeast(0)) {
                onElegirTema(ProgramaModel.TEMAS[it])
            }
        }
        item(key = "acciones") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onActualizar, enabled = !state.cargando) { Text("Actualizar") }
                if (state.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.avisoOffline) {
                    Text(
                        text = "Sin señal: se están usando datos guardados.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                when {
                    state.sinDescarga -> {
                        Text("No se pudo consultar el programa.")
                        OutlinedButton(onClick = onActualizar) { Text("Reintentar") }
                    }
                    state.sinCharlas -> Text("Sin charlas programadas.")
                    state.visibles.isEmpty() && !state.cargando -> {
                        Text("No hay charlas para los filtros elegidos.")
                    }
                }
            }
        }
        items(fichas, key = { it.id }) { ficha ->
            FichaCard(ficha, onAbrir)
        }
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(fichas, key = { it.id }) { ficha ->
            FichaCard(ficha, abrir)
        }
    }
}

@Composable
private fun FichaCard(ficha: Charla, abrir: (String) -> Unit) {
    ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
            Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
            Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
        }
    }
}
