package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(
    val id: String?,
    val title: String?,
    val track: String?,
    val day: Int?,
    val time: String?,
    val room: String?,
    val speaker: String?,
    val summary: String?
)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "sincronizacion")
data class Sincronizacion(
    @PrimaryKey val id: Int = 1,
    val momento: Long
)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT * FROM sincronizacion WHERE id = 1")
    abstract fun observarSincronizacion(): Flow<Sincronizacion?>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    abstract suspend fun favoritasActuales(): List<String>

    @Query("DELETE FROM charlas")
    abstract suspend fun borrarTodas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(fichas: List<Charla>)

    @Upsert
    abstract suspend fun marcarSincronizado(sincronizacion: Sincronizacion)

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)

    @Transaction
    open suspend fun reemplazarPrograma(remotas: List<Charla>, momento: Long) {
        val favoritas = favoritasActuales().toSet()
        borrarTodas()
        guardar(remotas.map { it.copy(favorita = it.id in favoritas) })
        marcarSincronizado(Sincronizacion(id = 1, momento = momento))
    }
}

@Database(entities = [Charla::class, Sincronizacion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao

    companion object {
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sincronizacion` (`id` INTEGER NOT NULL, `momento` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
