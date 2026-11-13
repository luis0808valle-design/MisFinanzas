package com.luisvalle.misfinanzas

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luisvalle.misfinanzas.data.AppDatabase
import com.luisvalle.misfinanzas.data.Movimiento
import com.luisvalle.misfinanzas.data.Programado
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val catsPago =
    listOf("Comida", "Transporte", "Hogar", "Servicios", "Salud", "Ocio", "Otros")
private val catsCobro = listOf("Salario", "Extra", "Otros")
private val repeticiones = listOf(
    "NINGUNA" to "Una vez",
    "SEMANAL" to "Cada semana",
    "QUINCENAL" to "Cada 2 semanas",
    "MENSUAL" to "Cada mes"
)
private val avisos = listOf(
    0 to "El mismo día", 1 to "1 día antes", 3 to "3 días antes", 7 to "7 días antes"
)
private val horas = listOf(8, 9, 12, 18, 20)

private fun etiquetaRep(clave: String) = repeticiones.first { it.first == clave }.second

private fun siguienteFecha(fecha: String, rep: String): String {
    val f = LocalDate.parse(fecha)
    return when (rep) {
        "SEMANAL" -> f.plusWeeks(1)
        "QUINCENAL" -> f.plusWeeks(2)
        "MENSUAL" -> f.plusMonths(1)
        else -> f
    }.toString()
}

class ProgramadosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.obtener(app).movimientoDao()
    private val contexto: Application = app

    val lista: StateFlow<List<Programado>> = dao.programados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregar(p: Programado) {
        viewModelScope.launch {
            val id = dao.insertarProgramado(p)
            Recordatorios.programar(contexto, p.copy(id = id))
        }
    }

    fun eliminar(p: Programado) {
        viewModelScope.launch {
            Recordatorios.cancelar(contexto, p)
            dao.eliminarProgramado(p)
        }
    }

    fun hecho(p: Programado) {
        viewModelScope.launch {
            dao.insertar(
                Movimiento(
                    esGasto = p.esGasto,
                    monto = p.monto,
                    categoria = p.categoria,
                    nota = p.nombre,
                    fecha = LocalDate.now().toString()
                )
            )
            if (p.repeticion == "NINGUNA") {
                Recordatorios.cancelar(contexto, p)
                dao.actualizarProgramado(p.copy(activo = false))
            } else {
                val nuevo = p.copy(fecha = siguienteFecha(p.fecha, p.repeticion))
                dao.actualizarProgramado(nuevo)
                Recordatorios.programar(contexto, nuevo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(fecha: LocalDate, alCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Fecha: $fecha")
    }
    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis =
                fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        alCambiar(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}

@Composable
fun PantallaProgramados(
    modifier: Modifier = Modifier,
    vm: ProgramadosViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val lanzador =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val lista by vm.lista.collectAsState()
    var esGasto by remember { mutableStateOf(true) }
    var nombre by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(catsPago[0]) }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var repeticion by remember { mutableStateOf("NINGUNA") }
    var diasAntes by remember { mutableIntStateOf(1) }
    var hora by remember { mutableIntStateOf(9) }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val categorias = if (esGasto) catsPago else catsCobro

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Programados",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Tarjeta() {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nuevo recordatorio", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = esGasto,
                            onClick = { esGasto = true; categoria = catsPago[0] },
                            label = { Text("Pago") }
                        )
                        FilterChip(
                            selected = !esGasto,
                            onClick = { esGasto = false; categoria = catsCobro[0] },
                            label = { Text("Entrada") }
                        )
                    }
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it; error = false },
                        label = { Text(if (esGasto) "¿Qué vas a pagar?" else "¿Qué vas a cobrar?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it; error = false },
                        label = { Text("Monto") },
                        isError = error,
                        supportingText = {
                            if (error) Text("Escribe un nombre y un monto mayor que cero.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categorias) { c ->
                            FilterChip(
                                selected = categoria == c,
                                onClick = { categoria = c },
                                label = { Text(c) }
                            )
                        }
                    }
                    SelectorFecha(fecha) { fecha = it }
                    Text("Repetir", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(repeticiones) { par ->
                            FilterChip(
                                selected = repeticion == par.first,
                                onClick = { repeticion = par.first },
                                label = { Text(par.second) }
                            )
                        }
                    }
                    Text("Avisar", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(avisos) { par ->
                            FilterChip(
                                selected = diasAntes == par.first,
                                onClick = { diasAntes = par.first },
                                label = { Text(par.second) }
                            )
                        }
                    }
                    Text("A las", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(horas) { h ->
                            FilterChip(
                                selected = hora == h,
                                onClick = { hora = h },
                                label = { Text("$h:00") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = mensaje,
                        onValueChange = { mensaje = it },
                        label = { Text("Mensaje del aviso (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val valor = monto.replace(',', '.').toDoubleOrNull()
                            if (nombre.isBlank() || valor == null || valor <= 0) {
                                error = true
                            } else {
                                vm.agregar(
                                    Programado(
                                        esGasto = esGasto,
                                        nombre = nombre.trim(),
                                        monto = valor,
                                        categoria = categoria,
                                        fecha = fecha.toString(),
                                        repeticion = repeticion,
                                        diasAntes = diasAntes,
                                        hora = hora,
                                        minuto = 0,
                                        mensaje = mensaje.trim(),
                                        activo = true
                                    )
                                )
                                nombre = ""
                                monto = ""
                                mensaje = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Guardar recordatorio") }
                    OutlinedButton(
                        onClick = {
                            Recordatorios.mostrar(
                                contexto,
                                Programado(
                                    id = 0, esGasto = true, nombre = "Prueba", monto = 1500.0,
                                    categoria = "Otros", fecha = LocalDate.now().toString(),
                                    repeticion = "NINGUNA", diasAntes = 0, hora = 9, minuto = 0,
                                    mensaje = "Así se verá tu recordatorio", activo = true
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Probar aviso ahora") }
                }
            }
        }

        item { Text("Pendientes", style = MaterialTheme.typography.titleMedium) }

        if (lista.isEmpty()) {
            item { Text("No tienes pagos ni entradas programadas.") }
        }

        items(lista, key = { it.id }) { p ->
            val dias = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(p.fecha))
            val estado = when {
                dias < 0 -> "Vencido hace ${-dias} d"
                dias == 0L -> "Hoy"
                else -> "En $dias d"
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            (if (p.esGasto) "Pagar: " else "Cobrar: ") + p.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatoDinero(p.monto))
                    }
                    Text(
                        "${p.fecha} · $estado · ${etiquetaRep(p.repeticion)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { vm.hecho(p) }) {
                            Text(if (p.esGasto) "Pagado" else "Cobrado")
                        }
                        TextButton(onClick = { vm.eliminar(p) }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
}