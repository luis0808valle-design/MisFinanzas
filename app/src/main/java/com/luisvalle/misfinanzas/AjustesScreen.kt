package com.luisvalle.misfinanzas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val estilos = listOf("cristal" to "Cristal", "clasico" to "Clásico")
private val temas = listOf("auto" to "Automático", "claro" to "Claro", "oscuro" to "Oscuro")
private val fuentes = listOf(
    "predeterminada" to "Predeterminada",
    "clasica" to "Clásica",
    "moderna" to "Moderna",
    "maquina" to "Máquina"
)

@Composable
private fun CampoPin(valor: String, etiqueta: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) alCambiar(it) },
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TarjetaApariencia() {
    val ctx = LocalContext.current
    var nombreTxt by remember { mutableStateOf(Apariencia.nombre) }
    var usuarioTxt by remember { mutableStateOf(Apariencia.usuario) }
    var monedaTxt by remember { mutableStateOf(Apariencia.moneda) }
    val cristal = Apariencia.estilo == "cristal"

    Tarjeta {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = usuarioTxt,
                onValueChange = {
                    if (it.length <= 20) {
                        usuarioTxt = it
                        Apariencia.cambiarUsuario(ctx, it)
                    }
                },
                label = { Text("Tu nombre (para el saludo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nombreTxt,
                onValueChange = {
                    if (it.length <= 30) {
                        nombreTxt = it
                        Apariencia.cambiarNombre(ctx, it)
                    }
                },
                label = { Text("Nombre de la app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Estilo", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estilos) { par ->
                    FilterChip(
                        selected = Apariencia.estilo == par.first,
                        onClick = { Apariencia.cambiarEstilo(ctx, par.first) },
                        label = { Text(par.second) }
                    )
                }
            }

            if (cristal) {
                Text("Fondo", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(Apariencia.fondos) { f ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                Apariencia.cambiarFondo(ctx, f.clave)
                            }
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(f.c1), Color(f.c3))
                                        )
                                    )
                                    .then(
                                        if (Apariencia.fondo == f.clave)
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            )
                                        else Modifier
                                    )
                            )
                            Text(f.nombre, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Transparencia del cristal", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = Apariencia.transparencia,
                    onValueChange = { Apariencia.cambiarTransparencia(ctx, it) },
                    valueRange = 0.06f..0.35f
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Luces de fondo")
                    Switch(
                        checked = Apariencia.luces,
                        onCheckedChange = { Apariencia.cambiarLuces(ctx, it) }
                    )
                }
            } else {
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(temas) { par ->
                        FilterChip(
                            selected = Apariencia.tema == par.first,
                            onClick = { Apariencia.cambiarTema(ctx, par.first) },
                            label = { Text(par.second) }
                        )
                    }
                }
            }

            Text("Color principal", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Apariencia.paleta) { c ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(
                                if (Apariencia.color == c)
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                else Modifier
                            )
                            .clickable { Apariencia.cambiarColor(ctx, c) }
                    )
                }
            }
            Text("Tipo de letra", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fuentes) { par ->
                    FilterChip(
                        selected = Apariencia.fuente == par.first,
                        onClick = { Apariencia.cambiarFuente(ctx, par.first) },
                        label = { Text(par.second) }
                    )
                }
            }
            OutlinedTextField(
                value = monedaTxt,
                onValueChange = {
                    if (it.length <= 5) {
                        monedaTxt = it
                        Apariencia.cambiarMoneda(ctx, it)
                    }
                },
                label = { Text("Símbolo de moneda") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PantallaAjustes(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var hayPin by remember { mutableStateOf(Seguridad.tienePin(ctx)) }
    var huella by remember { mutableStateOf(Seguridad.huellaActiva(ctx)) }
    var actual by remember { mutableStateOf("") }
    var nuevo by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    fun limpiar() {
        actual = ""
        nuevo = ""
        confirmar = ""
    }

    fun falloActual(): String {
        val s = Seguridad.segundosBloqueo(ctx)
        return if (s > 0) "Demasiados intentos. Espera $s s." else "El PIN actual no es correcto."
    }

    fun guardar() {
        if (hayPin && !Seguridad.verificar(ctx, actual)) {
            mensaje = falloActual()
        } else if (!Seguridad.pinValido(nuevo)) {
            mensaje = "El PIN debe tener de 4 a 6 dígitos."
        } else if (nuevo != confirmar) {
            mensaje = "Los PIN no coinciden."
        } else {
            Seguridad.guardarPin(ctx, nuevo)
            hayPin = true
            limpiar()
            mensaje = "PIN guardado."
        }
    }

    fun quitar() {
        if (Seguridad.verificar(ctx, actual)) {
            Seguridad.quitarPin(ctx)
            hayPin = false
            huella = false
            limpiar()
            mensaje = "PIN desactivado."
        } else {
            mensaje = falloActual()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item { TarjetaApariencia() }
        item {
            Tarjeta {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Seguridad", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hayPin) "La app está protegida con PIN."
                        else "Protege tu app con un PIN de 4 a 6 dígitos."
                    )
                    if (hayPin) CampoPin(actual, "PIN actual") { actual = it }
                    CampoPin(nuevo, if (hayPin) "PIN nuevo" else "PIN (4 a 6 dígitos)") { nuevo = it }
                    CampoPin(confirmar, "Confirmar PIN") { confirmar = it }
                    if (mensaje.isNotEmpty()) {
                        Text(mensaje, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { guardar() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (hayPin) "Cambiar PIN" else "Activar PIN") }
                    if (hayPin) {
                        OutlinedButton(
                            onClick = { quitar() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Quitar PIN (escribe el PIN actual)") }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entrar con huella")
                            Switch(
                                checked = huella,
                                onCheckedChange = { quiere ->
                                    if (quiere && !Seguridad.huellaDisponible(ctx)) {
                                        mensaje = "Este teléfono no tiene una huella registrada."
                                    } else {
                                        Seguridad.setHuella(ctx, quiere)
                                        huella = quiere
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}