package com.luisvalle.misfinanzas

import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luisvalle.misfinanzas.data.AppDatabase
import com.luisvalle.misfinanzas.data.Movimiento
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var bloqueada by mutableStateOf(false)
    private var ultimaSalida = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Apariencia.cargar(this)
        Recordatorios.crearCanal(this)
        Thread { Recordatorios.reprogramarTodo(applicationContext) }.start()
        bloqueada = Seguridad.tienePin(this)
        setContent {
            TemaApp {
                if (bloqueada) PantallaBloqueo { bloqueada = false }
                else Principal()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ultimaSalida = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (Seguridad.tienePin(this) && !bloqueada && ultimaSalida > 0 &&
            SystemClock.elapsedRealtime() - ultimaSalida > 30_000
        ) bloqueada = true
    }
}

@Composable
private fun BarraNav(
    actual: Int,
    items: List<Pair<String, String>>,
    fondo: Color,
    insets: WindowInsets,
    alElegir: (Int) -> Unit
) {
    NavigationBar(containerColor = fondo, windowInsets = insets) {
        items.forEachIndexed { i, par ->
            NavigationBarItem(
                selected = actual == i,
                onClick = { alElegir(i) },
                icon = { Text(par.first) },
                label = { Text(par.second) }
            )
        }
    }
}

@Composable
private fun Principal() {
    var pestana by remember { mutableIntStateOf(0) }
    val pestanas = listOf("💸" to "Movimientos", "🔔" to "Programados", "📊" to "Reportes", "⚙️" to "Ajustes")
    val cristal = Apariencia.estilo == "cristal"
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (cristal) Color.Transparent else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (cristal) {
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .cristal(RoundedCornerShape(28.dp))
                ) {
                    BarraNav(pestana, pestanas, Color.Transparent, WindowInsets(0, 0, 0, 0)) {
                        pestana = it
                    }
                }
            } else {
                BarraNav(
                    pestana,
                    pestanas,
                    MaterialTheme.colorScheme.surfaceContainer,
                    NavigationBarDefaults.windowInsets
                ) { pestana = it }
            }
        }
    ) { padding ->
        when (pestana) {
            0 -> PantallaMovimientos(Modifier.padding(padding))
            1 -> PantallaProgramados(Modifier.padding(padding))
            2 -> PantallaReportes(Modifier.padding(padding)); else -> PantallaAjustes(Modifier.padding(padding))
        }
    }
}

class MovimientosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.obtener(app).movimientoDao()

    val movimientos: StateFlow<List<Movimiento>> = dao.todos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregar(m: Movimiento) {
        viewModelScope.launch { dao.insertar(m) }
    }

    fun eliminar(m: Movimiento) {
        viewModelScope.launch { dao.eliminar(m) }
    }
}

private val categoriasGasto =
    listOf("Comida", "Transporte", "Hogar", "Servicios", "Salud", "Ocio", "Otros")
private val categoriasEntrada = listOf("Salario", "Extra", "Otros")
private val verde = Color(0xFF22C55E)
private val rojo = Color(0xFFEF4444)

private fun dinero(valor: Double): String =
    Apariencia.moneda + " " + String.format(Locale.forLanguageTag("es-DO"), "%,.2f", valor)

private fun emoji(categoria: String): String = when (categoria) {
    "Comida" -> "🍽️"
    "Transporte" -> "🚌"
    "Hogar" -> "🏠"
    "Servicios" -> "💡"
    "Salud" -> "🩺"
    "Ocio" -> "🎉"
    "Salario" -> "💼"
    "Extra" -> "✨"
    else -> "📦"
}

@Composable
private fun Encabezado() {
    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF14532D)))
                )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.55f)
            )
        }
        Column {
            Text(
                if (Apariencia.usuario.isBlank()) "Hola 👋" else "Hola, ${Apariencia.usuario} 👋",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "${Apariencia.nombre} · tu dinero, bajo control",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaBalance(entradas: Double, gastos: Double) {
    val base = Color(Apariencia.color)
    val cristal = Apariencia.estilo == "cristal"
    val forma = RoundedCornerShape(28.dp)
    val relleno =
        if (cristal) Brush.linearGradient(listOf(base.copy(alpha = 0.7f), base.copy(alpha = 0.25f)))
        else Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.55f)))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(relleno)
            .then(if (cristal) Modifier.border(1.dp, BordeCristal, forma) else Modifier)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Balance total",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    dinero(entradas - gastos),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_bolsa),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun MiniTarjeta(
    modifier: Modifier,
    titulo: String,
    monto: Double,
    flecha: String,
    tono: Color
) {
    Tarjeta(modifier) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tono),
                contentAlignment = Alignment.Center
            ) {
                Text(flecha, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    titulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(dinero(monto), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun PantallaMovimientos(
    modifier: Modifier = Modifier,
    vm: MovimientosViewModel = viewModel()
) {
    val lista by vm.movimientos.collectAsState()
    var esGasto by remember { mutableStateOf(true) }
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(categoriasGasto[0]) }
    var nota by remember { mutableStateOf("") }
    var montoInvalido by remember { mutableStateOf(false) }
    var filtro by remember { mutableIntStateOf(0) }

    val entradas = lista.filter { !it.esGasto }.sumOf { it.monto }
    val gastos = lista.filter { it.esGasto }.sumOf { it.monto }
    val categorias = if (esGasto) categoriasGasto else categoriasEntrada
    val visibles = when (filtro) {
        1 -> lista.filter { !it.esGasto }
        2 -> lista.filter { it.esGasto }
        else -> lista
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Encabezado() }
        item { TarjetaBalance(entradas, gastos) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniTarjeta(Modifier.weight(1f), "Ingresos", entradas, "↓", verde)
                MiniTarjeta(Modifier.weight(1f), "Gastos", gastos, "↑", rojo)
            }
        }

        item {
            Tarjeta {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nuevo movimiento", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = esGasto,
                            onClick = { esGasto = true; categoria = categoriasGasto[0] },
                            label = { Text("Gasto") }
                        )
                        FilterChip(
                            selected = !esGasto,
                            onClick = { esGasto = false; categoria = categoriasEntrada[0] },
                            label = { Text("Ingreso") }
                        )
                    }
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it; montoInvalido = false },
                        label = { Text("Monto") },
                        isError = montoInvalido,
                        supportingText = {
                            if (montoInvalido) Text("Escribe un monto mayor que cero.")
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
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        label = { Text("Nota (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val valor = monto.replace(',', '.').toDoubleOrNull()
                            if (valor == null || valor <= 0) {
                                montoInvalido = true
                            } else {
                                vm.agregar(
                                    Movimiento(
                                        esGasto = esGasto,
                                        monto = valor,
                                        categoria = categoria,
                                        nota = nota.trim(),
                                        fecha = LocalDate.now().toString()
                                    )
                                )
                                monto = ""
                                nota = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Guardar movimiento") }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Historial", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todo", "Ingresos", "Gastos").forEachIndexed { i, texto ->
                        FilterChip(
                            selected = filtro == i,
                            onClick = { filtro = i },
                            label = { Text(texto) }
                        )
                    }
                }
            }
        }

        if (visibles.isEmpty()) {
            item { Text("No hay movimientos para mostrar todavía.") }
        }

        items(visibles, key = { it.id }) { m ->
            Tarjeta {
                Row(
                    Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background((if (m.esGasto) rojo else verde).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji(m.categoria)) }
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.nota.ifBlank { m.categoria },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${m.categoria} · ${m.fecha}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        (if (m.esGasto) "−" else "+") + dinero(m.monto),
                        color = if (m.esGasto) rojo else verde,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { vm.eliminar(m) }) { Text("✕") }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}