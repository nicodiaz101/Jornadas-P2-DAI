package ar.jornadas.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa: Flow<List<Charla>> = dao.observar()
    val hayEdicion: Flow<Boolean> = combine(dao.observarSincronizacion(), programa) { sinc, lista ->
        sinc != null || lista.isNotEmpty()
    }

    suspend fun sincronizar(): Result<Unit> = try {
        val remotas = api.consultar()
        val fichas = remotas.map { it.aCharla() }.distinctBy { it.id }
        dao.reemplazarPrograma(fichas, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)

    private fun FichaRemota.aCharla(): Charla = Charla(
        id = requireNotNull(id?.takeIf { it.isNotBlank() }) { "id inválido" },
        titulo = requireNotNull(title) { "title nulo" },
        tema = requireNotNull(track) { "track nulo" },
        dia = requireNotNull(day) { "day nulo" },
        hora = requireNotNull(time) { "time nulo" },
        sala = requireNotNull(room) { "room nulo" },
        ponente = requireNotNull(speaker) { "speaker nulo" },
        resumen = requireNotNull(summary) { "summary nulo" }
    )
}
