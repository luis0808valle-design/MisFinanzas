package com.luisvalle.misfinanzas

import android.content.Context
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class FondoDef(
    val clave: String,
    val nombre: String,
    val c1: Int,
    val c2: Int,
    val c3: Int
)

object Apariencia {
    val paleta = listOf(
        0xFF0F766E.toInt(), 0xFF2563EB.toInt(), 0xFF7C3AED.toInt(), 0xFFDB2777.toInt(),
        0xFFEA580C.toInt(), 0xFFB45309.toInt(), 0xFF15803D.toInt(), 0xFF475569.toInt()
    )

    val fondos = listOf(
        FondoDef("aurora", "Aurora", 0xFF1E40AF.toInt(), 0xFF6D28D9.toInt(), 0xFFA78BFA.toInt()),
        FondoDef("esmeralda", "Esmeralda", 0xFF064E3B.toInt(), 0xFF0F766E.toInt(), 0xFF34D399.toInt()),
        FondoDef("atardecer", "Atardecer", 0xFF9D174D.toInt(), 0xFFC2410C.toInt(), 0xFFFBBF24.toInt()),
        FondoDef("oceano", "Océano", 0xFF0C4A6E.toInt(), 0xFF1E40AF.toInt(), 0xFF22D3EE.toInt()),
        FondoDef("medianoche", "Medianoche", 0xFF0B1020.toInt(), 0xFF1E1B4B.toInt(), 0xFF6366F1.toInt())
    )

    var nombre by mutableStateOf("Mis Finanzas")
        private set
    var usuario by mutableStateOf("")
        private set
    var estilo by mutableStateOf("cristal")
        private set
    var fondo by mutableStateOf("aurora")
        private set
    var transparencia by mutableFloatStateOf(0.16f)
        private set
    var luces by mutableStateOf(true)
        private set
    var tema by mutableStateOf("auto")
        private set
    var color by mutableStateOf(paleta[0])
        private set
    var fuente by mutableStateOf("predeterminada")
        private set
    var moneda by mutableStateOf("RD$")
        private set

    fun fondoActual(): FondoDef = fondos.firstOrNull { it.clave == fondo } ?: fondos[0]

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("apariencia", Context.MODE_PRIVATE)

    fun cargar(ctx: Context) {
        val p = prefs(ctx)
        nombre = p.getString("nombre", "Mis Finanzas") ?: "Mis Finanzas"
        usuario = p.getString("usuario", "") ?: ""
        estilo = p.getString("estilo", "cristal") ?: "cristal"
        fondo = p.getString("fondo", "aurora") ?: "aurora"
        transparencia = p.getFloat("transparencia", 0.16f)
        luces = p.getBoolean("luces", true)
        tema = p.getString("tema", "auto") ?: "auto"
        color = p.getInt("color", paleta[0])
        fuente = p.getString("fuente", "predeterminada") ?: "predeterminada"
        moneda = p.getString("moneda", "RD$") ?: "RD$"
    }

    fun cambiarNombre(ctx: Context, v: String) {
        nombre = v.trim().ifBlank { "Mis Finanzas" }
        prefs(ctx).edit().putString("nombre", nombre).apply()
    }

    fun cambiarUsuario(ctx: Context, v: String) {
        usuario = v.trim()
        prefs(ctx).edit().putString("usuario", usuario).apply()
    }

    fun cambiarEstilo(ctx: Context, v: String) {
        estilo = v
        prefs(ctx).edit().putString("estilo", v).apply()
    }

    fun cambiarFondo(ctx: Context, v: String) {
        fondo = v
        prefs(ctx).edit().putString("fondo", v).apply()
    }

    fun cambiarTransparencia(ctx: Context, v: Float) {
        transparencia = v
        prefs(ctx).edit().putFloat("transparencia", v).apply()
    }

    fun cambiarLuces(ctx: Context, v: Boolean) {
        luces = v
        prefs(ctx).edit().putBoolean("luces", v).apply()
    }

    fun cambiarTema(ctx: Context, v: String) {
        tema = v
        prefs(ctx).edit().putString("tema", v).apply()
    }

    fun cambiarColor(ctx: Context, v: Int) {
        color = v
        prefs(ctx).edit().putInt("color", v).apply()
    }

    fun cambiarFuente(ctx: Context, v: String) {
        fuente = v
        prefs(ctx).edit().putString("fuente", v).apply()
    }

    fun cambiarMoneda(ctx: Context, v: String) {
        moneda = v.trim().ifBlank { "RD$" }
        prefs(ctx).edit().putString("moneda", moneda).apply()
    }
}

val BordeCristal = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))
)

fun Modifier.cristal(forma: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.clip(forma)
        .background(Color.White.copy(alpha = Apariencia.transparencia))
        .border(1.dp, BordeCristal, forma)

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier.fillMaxWidth(),
    contenido: @Composable ColumnScope.() -> Unit
) {
    if (Apariencia.estilo == "cristal") {
        Column(modifier = modifier.cristal(), content = contenido)
    } else {
        Card(modifier = modifier, content = contenido)
    }
}

private fun Modifier.fondoCristal(): Modifier = this.drawBehind {
    val f = Apariencia.fondoActual()
    drawRect(
        Brush.linearGradient(
            listOf(Color(f.c1), Color(f.c2)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
    if (Apariencia.luces) {
        val centro1 = Offset(size.width * 0.85f, size.height * 0.10f)
        val radio1 = size.width * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(f.c3).copy(alpha = 0.6f), Color.Transparent),
                center = centro1,
                radius = radio1
            ),
            radius = radio1,
            center = centro1
        )
        val centro2 = Offset(size.width * 0.05f, size.height * 0.80f)
        val radio2 = size.width * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(Apariencia.color).copy(alpha = 0.5f), Color.Transparent),
                center = centro2,
                radius = radio2
            ),
            radius = radio2,
            center = centro2
        )
    }
}

private fun familia(clave: String): FontFamily = when (clave) {
    "clasica" -> FontFamily.Serif
    "moderna" -> FontFamily.SansSerif
    "maquina" -> FontFamily.Monospace
    else -> FontFamily.Default
}

private fun tipografia(f: FontFamily): Typography {
    val t = Typography()
    val n = FontWeight.Bold
    val s = FontWeight.SemiBold
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = f, fontWeight = n),
        displayMedium = t.displayMedium.copy(fontFamily = f, fontWeight = n),
        displaySmall = t.displaySmall.copy(fontFamily = f, fontWeight = n),
        headlineLarge = t.headlineLarge.copy(fontFamily = f, fontWeight = n),
        headlineMedium = t.headlineMedium.copy(fontFamily = f, fontWeight = n),
        headlineSmall = t.headlineSmall.copy(fontFamily = f, fontWeight = n),
        titleLarge = t.titleLarge.copy(fontFamily = f, fontWeight = s),
        titleMedium = t.titleMedium.copy(fontFamily = f, fontWeight = s),
        titleSmall = t.titleSmall.copy(fontFamily = f, fontWeight = s),
        bodyLarge = t.bodyLarge.copy(fontFamily = f),
        bodyMedium = t.bodyMedium.copy(fontFamily = f),
        bodySmall = t.bodySmall.copy(fontFamily = f),
        labelLarge = t.labelLarge.copy(fontFamily = f, fontWeight = s),
        labelMedium = t.labelMedium.copy(fontFamily = f),
        labelSmall = t.labelSmall.copy(fontFamily = f)
    )
}

private val formas = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun TemaApp(contenido: @Composable () -> Unit) {
    val cristal = Apariencia.estilo == "cristal"
    val oscuro = cristal || when (Apariencia.tema) {
        "claro" -> false
        "oscuro" -> true
        else -> isSystemInDarkTheme()
    }
    val base = Color(Apariencia.color)
    val ctx = LocalContext.current

    SideEffect {
        ctx.comoActividad()?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { oscuro }
        )
    }

    val esquema = if (cristal) {
        darkColorScheme(
            primary = lerp(base, Color.White, 0.45f),
            onPrimary = Color(0xFF0B1020),
            primaryContainer = base.copy(alpha = 0.55f),
            onPrimaryContainer = Color.White,
            secondaryContainer = Color.White.copy(alpha = 0.26f),
            onSecondaryContainer = Color.White,
            background = Color(0xFF0B1020),
            onBackground = Color.White,
            surface = Color(0xFF0B1020),
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.72f),
            surfaceContainer = Color(0xFF1B2145),
            surfaceContainerHigh = Color(0xFF1B2145),
            surfaceContainerHighest = Color(0xFF232A55),
            outline = Color.White.copy(alpha = 0.4f),
            outlineVariant = Color.White.copy(alpha = 0.2f)
        )
    } else if (oscuro) {
        darkColorScheme(
            primary = lerp(base, Color.White, 0.4f),
            onPrimary = Color(0xFF101414),
            primaryContainer = lerp(base, Color.Black, 0.5f),
            onPrimaryContainer = Color.White,
            secondaryContainer = lerp(base, Color.Black, 0.6f),
            onSecondaryContainer = Color.White,
            background = Color(0xFF0E1312),
            surface = Color(0xFF0E1312),
            surfaceContainer = Color(0xFF161C1B),
            surfaceContainerHigh = Color(0xFF1A2120),
            surfaceContainerHighest = Color(0xFF1F2726)
        )
    } else {
        lightColorScheme(
            primary = base,
            onPrimary = Color.White,
            primaryContainer = lerp(base, Color.White, 0.8f),
            onPrimaryContainer = lerp(base, Color.Black, 0.5f),
            secondaryContainer = lerp(base, Color.White, 0.8f),
            onSecondaryContainer = lerp(base, Color.Black, 0.5f),
            background = Color(0xFFF4F6F5),
            surface = Color(0xFFF4F6F5),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFFFFFFF),
            surfaceContainerHighest = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = esquema,
        typography = tipografia(familia(Apariencia.fuente)),
        shapes = formas
    ) {
        Surface(
            modifier = if (cristal) Modifier.fillMaxSize().fondoCristal()
            else Modifier.fillMaxSize(),
            color = if (cristal) Color.Transparent else MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) { contenido() }
    }
}