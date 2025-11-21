package com.example.rehabook.pantallas.auth

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
import com.example.rehabook.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import android.util.Log
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

@Composable
fun RegisterScreen(auth: FirebaseAuth, database: DatabaseReference, navController: NavController) {
    Log.d("RehabookInit","RegisterScreen está siendo compuesta.")
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
                        Log.d("RehabookRegister", "Iniciando proceso de registro para: $email")
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    Log.d("RehabookRegister", "Autenticación exitosa. UID: ${auth.currentUser?.uid}") // Log 2
                                    val currentUser = auth.currentUser
                                    currentUser?.let { user ->
                                        val uid = user.uid
                                        val nuevoUsuario = Usuario(
                                            nombre = name,
                                            email = email,
                                            telefono = phone,
                                            dni = dni
                                        )
                                        Log.d("RehabookRegister", "Intentando guardar datos en RTDB para UID: $uid") // Log 3
                                        Log.d("RehabookRegister", "Datos a guardar: $nuevoUsuario") // Log 4
                                        
                                        database.child("usuario").child(uid).setValue(nuevoUsuario)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                                Log.d("RehabookRegister", "Datos de usuario guardados exitosamente en RTDB.") // Log 5
                                                navController.navigate(Screen.Login.route) {
                                                    popUpTo(Screen.Register.route) { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { dbError ->
                                                Toast.makeText(context, "Error guardando datos del usuario: ${dbError.message}", Toast.LENGTH_LONG).show()
                                                Log.e("RehabookRegister", "ERROR al guardar datos en RTDB: ${dbError.message}", dbError) // Log 6
                                                auth.currentUser?.delete()
                                            }
                                            .addOnCompleteListener { task ->
                                                if(task.isSuccessful){
                                                    Log.d("RehabookRegister", "Log7 (COMPLETER): La tarea setValue se completó con ÉXITO. Datos guardados: $nuevoUsuario")
                                                }else{
                                                    Log.d("RehabookRegister", "Log7 (COMPLETER): La tarea setValue falló. Excepción: ${task.exception?.message}",task.exception)
                                                }
                                            }
                                    } ?: run {
                                        Log.e("RehabookRegister", "ERROR: currentUser es nulo DESPUÉS de autenticación exitosa.") // Log 7
                                        Toast.makeText(context, "Usuario autenticado nulo después del registro", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val errorMessage = when (authTask.exception){
                                        is FirebaseAuthUserCollisionException -> "El correo ya está en uso"
                                        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado debil"
                                        is FirebaseAuthInvalidCredentialsException -> "Formato de correo no válido"
                                        is FirebaseAuthInvalidUserException -> "Este usuario no existe o ha sido deshabilitado"
                                        else -> "Error al registrarse. Por favor, inténtalo de nuevo"
                                    }
                                    Log.e("RehabookRegister", "ERROR en autenticación: ${authTask.exception?.message}", authTask.exception) // Log 8
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
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
