package com.luisvalle.misfinanzas

import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.delay

object Seguridad {
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("seguridad", Context.MODE_PRIVATE)

    private fun calcular(pin: String, sal: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), sal, 120_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun tienePin(ctx: Context): Boolean = prefs(ctx).contains("hash")

    fun pinValido(pin: String): Boolean = pin.length in 4..6 && pin.all { it.isDigit() }

    fun guardarPin(ctx: Context, pin: String) {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val huella = prefs(ctx).getBoolean("huella", false)
        prefs(ctx).edit()
            .clear()
            .putString("sal", Base64.encodeToString(sal, Base64.NO_WRAP))
            .putString("hash", calcular(pin, sal))
            .putBoolean("huella", huella)
            .apply()
    }

    fun quitarPin(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    fun segundosBloqueo(ctx: Context): Int {
        val resto = prefs(ctx).getLong("bloqueoHasta", 0L) - System.currentTimeMillis()
        return if (resto > 0) ((resto + 999) / 1000).toInt() else 0
    }

    fun verificar(ctx: Context, pin: String): Boolean {
        if (segundosBloqueo(ctx) > 0) return false
        val p = prefs(ctx)
        val esperado = p.getString("hash", null) ?: return false
        val sal = Base64.decode(p.getString("sal", "") ?: "", Base64.NO_WRAP)
        val correcto = MessageDigest.isEqual(
            calcular(pin, sal).toByteArray(),
            esperado.toByteArray()
        )
        if (correcto) {
            p.edit().putInt("fallos", 0).putLong("bloqueoHasta", 0L).apply()
            return true
        }
        val fallos = p.getInt("fallos", 0) + 1
        val espera = if (fallos >= 5) minOf(30L * (1L shl minOf(fallos - 5, 4)), 600L) else 0L
        p.edit()
            .putInt("fallos", fallos)
            .putLong(
                "bloqueoHasta",
                if (espera > 0) System.currentTimeMillis() + espera * 1000 else 0L
            )
            .apply()
        return false
    }

    fun huellaActiva(ctx: Context): Boolean = prefs(ctx).getBoolean("huella", false)

    fun setHuella(ctx: Context, activa: Boolean) {
        prefs(ctx).edit().putBoolean("huella", activa).apply()
    }

    fun huellaDisponible(ctx: Context): Boolean =
        BiometricManager.from(ctx)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    fun pedirHuella(act: FragmentActivity, alExito: () -> Unit) {
        val aviso = BiometricPrompt(
            act,
            ContextCompat.getMainExecutor(act),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    alExito()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mis Finanzas")
            .setSubtitle("Confirma con tu huella")
            .setNegativeButtonText("Usar PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        aviso.authenticate(info)
    }
}

fun Context.comoActividad(): FragmentActivity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun PantallaBloqueo(alDesbloquear: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var restante by remember { mutableIntStateOf(Seguridad.segundosBloqueo(ctx)) }
    val conHuella = Seguridad.huellaActiva(ctx) && Seguridad.huellaDisponible(ctx)

    fun pedirHuella() {
        ctx.comoActividad()?.let { Seguridad.pedirHuella(it, alDesbloquear) }
    }

    LaunchedEffect(Unit) { if (conHuella) pedirHuella() }
    LaunchedEffect(restante) {
        if (restante > 0) {
            delay(1000)
            restante = Seguridad.segundosBloqueo(ctx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 48.sp)
        Text(Apariencia.nombre, style = MaterialTheme.typography.headlineMedium)
        Text("Escribe tu PIN para entrar")
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                    pin = it
                    mensaje = ""
                }
            },
            label = { Text("PIN") },
            singleLine = true,
            enabled = restante == 0,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        if (restante > 0) Text("Demasiados intentos. Espera $restante s.")
        else if (mensaje.isNotEmpty()) Text(mensaje)
        Button(
            onClick = {
                if (Seguridad.verificar(ctx, pin)) {
                    alDesbloquear()
                } else {
                    pin = ""
                    restante = Seguridad.segundosBloqueo(ctx)
                    mensaje = "PIN incorrecto."
                }
            },
            enabled = restante == 0 && pin.length >= 4
        ) { Text("Desbloquear") }
        if (conHuella) {
            OutlinedButton(onClick = { pedirHuella() }) { Text("Usar huella") }
        }
    }
}