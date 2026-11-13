package com.luisvalle.misfinanzas.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val esGasto: Boolean,
    val monto: Double,
    val categoria: String,
    val nota: String,
    val fecha: String
)

@Entity(tableName = "programados")
data class Programado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val esGasto: Boolean,
    val nombre: String,
    val monto: Double,
    val categoria: String,
    val fecha: String,
    val repeticion: String,
    val diasAntes: Int,
    val hora: Int,
    val minuto: Int,
    val mensaje: String,
    val activo: Boolean
)

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun todos(): Flow<List<Movimiento>>

    @Insert
    suspend fun insertar(movimiento: Movimiento)

    @Delete
    suspend fun eliminar(movimiento: Movimiento)

    @Query("SELECT * FROM programados WHERE activo = 1 ORDER BY fecha ASC, hora ASC")
    fun programados(): Flow<List<Programado>>

    @Query("SELECT * FROM programados WHERE activo = 1")
    fun programadosActivosSync(): List<Programado>

    @Query("SELECT * FROM programados WHERE id = :id")
    fun programadoPorIdSync(id: Long): Programado?

    @Insert
    suspend fun insertarProgramado(p: Programado): Long

    @Update
    suspend fun actualizarProgramado(p: Programado)

    @Delete
    suspend fun eliminarProgramado(p: Programado)
}

@Database(entities = [Movimiento::class, Programado::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programados` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`esGasto` INTEGER NOT NULL, " +
                            "`nombre` TEXT NOT NULL, " +
                            "`monto` REAL NOT NULL, " +
                            "`categoria` TEXT NOT NULL, " +
                            "`fecha` TEXT NOT NULL, " +
                            "`repeticion` TEXT NOT NULL, " +
                            "`diasAntes` INTEGER NOT NULL, " +
                            "`hora` INTEGER NOT NULL, " +
                            "`minuto` INTEGER NOT NULL, " +
                            "`mensaje` TEXT NOT NULL, " +
                            "`activo` INTEGER NOT NULL)"
                )
            }
        }

        fun obtener(contexto: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "finanzas.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
    }
}