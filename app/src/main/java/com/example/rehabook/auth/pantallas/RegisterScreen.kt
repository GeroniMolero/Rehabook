package com.example.rehabook.auth.pantallas

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.models.usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment

@Composable
fun RegisterScreen(auth: FirebaseAuth, database: DatabaseReference, navController: NavController) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var dni by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = dni,
            onValueChange = { dni = it },
            label = { Text("DNI") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Ocultar" else "Mostrar")
                }
            }
        )

        Button(
            onClick = {
                // Validaciones
                when {
                    name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || dni.isEmpty() -> {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        Toast.makeText(context, "Email no válido", Toast.LENGTH_SHORT).show()
                    }
                    password.length < 6 -> {
                        Toast.makeText(context, "Contraseña mínima 6 caracteres", Toast.LENGTH_SHORT).show()
                    }
                    phone.length != 9 -> {
                        Toast.makeText(context, "El telefono debe tener 9 digitos",Toast.LENGTH_SHORT).show()
                    }
                    !validarDni(dni) -> {
                        Toast.makeText(context, "DNI inválido", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    val currentUser = auth.currentUser
                                    currentUser?.let { user ->
                                        val uid = user.uid
                                        val nuevoUsuario = usuario(
                                            nombre = name,
                                            email = email,
                                            telefono = phone,
                                            dni = dni
                                        )

                                        database.child("usuarios").child(uid).setValue(nuevoUsuario)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                                navController.navigate(Screen.Home.route) {
                                                    popUpTo(Screen.Register.route) { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { dbError ->
                                                Toast.makeText(context, "Error guardando datos del usuario: ${dbError.message}", Toast.LENGTH_LONG).show()
                                                auth.currentUser?.delete()
                                            }
                                    } ?: run {
                                        Toast.makeText(context, "Usuario autenticado nulo después del registro", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // El registro de Authentication falló
                                    Toast.makeText(context, "Error al registrarse: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        // --- FIN DE CORRECCIONES ---
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        TextButton(
            onClick = { navController.navigate(Screen.Login.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ya tengo cuenta")
        }
    }
}
fun validarDni(dni: String): Boolean {
    val dniRegex = Regex("""^\d{8}[A-Za-z]$""")
    if (!dniRegex.matches(dni)) return false

    val letras = "TRWAGMYFPDXBNJZSQVHLCKE"
    val numero = dni.substring(0, 8).toInt()
    val letraCorrecta = letras[numero % 23]

    return dni.last().uppercaseChar() == letraCorrecta
}
