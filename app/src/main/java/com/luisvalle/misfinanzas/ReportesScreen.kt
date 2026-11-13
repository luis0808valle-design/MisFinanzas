package com.luisvalle.misfinanzas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as EstiloTexto
import java.util.Locale
import kotlin.math.roundToInt

private val verdeR = Color(0xFF22C55E)
private val rojoR = Color(0xFFEF4444)
private val idioma = Locale.forLanguageTag("es-DO")

private val coloresCategoria = mapOf(
    "Comida" to Color(0xFFF59E0B),
    "Transporte" to Color(0xFF3B82F6),
    "Hogar" to Color(0xFF8B5CF6),
    "Servicios" to Color(0xFF06B6D4),
    "Salud" to Color(0xFFEF4444),
    "Ocio" to Color(0xFFEC4899),
    "Otros" to Color(0xFF94A3B8)
)

private fun colorDe(categoria: String): Color =
    coloresCategoria[categoria] ?: Color(0xFF94A3B8)

private val consejos = listOf(
    "Ahorra al menos el 20% de tus ingresos mensuales.",
    "Anota cada gasto el mismo día: lo que no se anota, se olvida.",
    "Programa tus pagos fijos para no pagar recargos por atraso.",
    "Antes de una compra grande, espera 24 horas y vuelve a decidir.",
    "Revisa tus gastos de Ocio una vez por semana y ajusta si hace falta.",
    "Separa primero el ahorro y gasta después con lo que queda."
)

private fun nombreMes(ym: YearMonth): String =
    ym.month.getDisplayName(EstiloTexto.FULL, idioma).replaceFirstChar { it.uppercase() } +
            " " + ym.year

private fun abreviatura(ym: YearMonth): String =
    ym.month.getDisplayName(EstiloTexto.SHORT, idioma).replaceFirstChar { it.uppercase() }.take(3)

@Composable
private fun Barra(fraccion: Double, color: Color) {
    val alto = if (fraccion <= 0.0) 0.01f else fraccion.toFloat().coerceIn(0.03f, 1f)
    Box(
        Modifier
            .width(10.dp)
            .fillMaxHeight(alto)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@Composable
fun PantallaReportes(
    modifier: Modifier = Modifier,
    vm: MovimientosViewModel = viewModel()
) {
    val lista by vm.movimientos.collectAsState()
    var desplazamiento by remember { mutableIntStateOf(0) }

    val mes = YearMonth.now().plusMonths(desplazamiento.toLong())
    val porMes = lista.groupBy { YearMonth.parse(it.fecha.take(7)) }
    val delMes = porMes[mes].orEmpty()
    val ingresos = delMes.filter { !it.esGasto }.sumOf { it.monto }
    val gastos = delMes.filter { it.esGasto }.sumOf { it.monto }
    val ahorro = if (ingresos > 0) ((ingresos - gastos) / ingresos * 100).roundToInt() else 0
    val porCategoria = delMes
        .filter { it.esGasto }
        .groupBy { it.categoria }
        .map { it.key to it.value.sumOf { m -> m.monto } }
        .sortedByDescending { it.second }

    val meses = (5 downTo 0).map { mes.minusMonths(it.toLong()) }
    val datos = meses.map { m ->
        val l = porMes[m].orEmpty()
        l.filter { !it.esGasto }.sumOf { it.monto } to l.filter { it.esGasto }.sumOf { it.monto }
    }
    val maximo = (datos.maxOfOrNull { maxOf(it.first, it.second) } ?: 0.0).coerceAtLeast(1.0)
    val consejo = consejos[LocalDate.now().dayOfYear % consejos.size]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Reportes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { desplazamiento -= 1 }) { Text("◀") }
                Text(nombreMes(mes), style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = { desplazamiento += 1 },
                    enabled = desplazamiento < 0
                ) { Text("▶") }
            }
        }

        item {
            Tarjeta {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Ingresos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatoDinero(ingresos),
                            style = MaterialTheme.typography.titleSmall,
                            color = verdeR
                        )
                    }
                    Column {
                        Text(
                            "Gastos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatoDinero(gastos),
                            style = MaterialTheme.typography.titleSmall,
                            color = rojoR
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Ahorro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("$ahorro%", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        item {
            Tarjeta {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val grosor = 22.dp.toPx()
                                val lado = size.minDimension - grosor
                                val esquina = Offset(grosor / 2, grosor / 2)
                                val tam = Size(lado, lado)
                                if (gastos <= 0.0) {
                                    drawArc(
                                        color = Color.Gray.copy(alpha = 0.25f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = esquina,
                                        size = tam,
                                        style = Stroke(grosor)
                                    )
                                } else {
                                    var inicio = -90f
                                    porCategoria.forEach { par ->
                                        val barrido = (par.second / gastos * 360.0).toFloat()
                                        val dibujo =
                                            if (porCategoria.size > 1) maxOf(barrido - 2f, 0.5f)
                                            else barrido
                                        drawArc(
                                            color = colorDe(par.first),
                                            startAngle = inicio,
                                            sweepAngle = dibujo,
                                            useCenter = false,
                                            topLeft = esquina,
                                            size = tam,
                                            style = Stroke(grosor)
                                        )
                                        inicio += barrido
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$ahorro%", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Ahorro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (porCategoria.isEmpty()) {
                                Text("Sin gastos en este mes.")
                            }
                            porCategoria.take(5).forEach { par ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colorDe(par.first))
                                    )
                                    Column {
                                        Text(par.first, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            formatoDinero(par.second),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Tarjeta {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Últimos 6 meses", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("● Ingresos", color = verdeR, style = MaterialTheme.typography.bodySmall)
                            Text("● Gastos", color = rojoR, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        meses.forEachIndexed { i, m ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    Modifier.height(110.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Barra(datos[i].first / maximo, verdeR)
                                    Barra(datos[i].second / maximo, rojoR)
                                }
                                Text(
                                    abreviatura(m),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Tarjeta {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("💡", style = MaterialTheme.typography.headlineMedium)
                    Column {
                        Text("Consejo del día", style = MaterialTheme.typography.titleSmall)
                        Text(
                            consejo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}