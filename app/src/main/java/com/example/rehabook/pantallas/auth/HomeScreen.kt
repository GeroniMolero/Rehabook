package com.example.rehabook.pantallas.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    auth: FirebaseAuth,
    database: DatabaseReference, // <-- Recibe la referencia como parámetro
    navController: NavController
) {
    val context = LocalContext.current
    val user = auth.currentUser
    // NO se inicializa aquí, se usa la que llega por parámetro
    // val database = Firebase.database(...).reference  <-- ¡ELIMINAR ESTA LÍNEA!
    val scope = rememberCoroutineScope()

    val keepLoggedIn = remember { mutableStateOf(SessionManager.getKeepLoggedIn(context)) }

    // --- ESTADOS PARA EL CHAT ---
    var rolUsuario by remember { mutableStateOf(2) }
    var adminUid by remember { mutableStateOf<String?>(null) }
    var cargandoAdmin by remember { mutableStateOf(false) }

    // --- EFECTO PARA OBTENER ROL Y ADMIN UID ---
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            // 1. Obtener el rol del usuario actual
            database.child("usuario").child(uid).get()
                .addOnSuccessListener { snap ->
                    rolUsuario = snap.child("rol").getValue(Int::class.java) ?: 2
                    Log.d("HomeScreen", "Rol del usuario: $rolUsuario")
                }

            // 2. Si el usuario es normal, buscar el UID del administrador
            if (rolUsuario == 2) {
                cargandoAdmin = true
                val adminUidTemporal = "7cT0g6mjOQN4kjHVrXmp2rO9VzE2"

                database.child("usuario").child(adminUidTemporal).child("rol").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.getValue(Int::class.java) != 1) {
                            adminUid = null
                            Log.d("HomeScreen", "El UID proporcionado ya no es administrador")
                        } else {
                            adminUid = adminUidTemporal
                            Log.d("HomeScreen", "UID del administrador encontrado: $adminUidTemporal")
                        }
                        cargandoAdmin = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeScreen", "Error verificando administrador", e)
                        cargandoAdmin = false
                    }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.CitasList.route) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Citas") },
                    label = { Text("Citas") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (rolUsuario == 1) {
                            navController.navigate(Screen.ChatList.route)
                        } else {
                            if (cargandoAdmin) {
                                Toast.makeText(context, "Buscando administrador...", Toast.LENGTH_SHORT).show()
                            } else {
                                adminUid?.let { uid ->
                                    Log.d("HomeScreen", "Navegando al chat con UID: $uid")
                                    navController.navigate(Screen.Chat.route.replace("{otherUserId}", uid))
                                } ?: run {
                                    Toast.makeText(context, "No se encontró administrador", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("Soporte") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido, ${user?.email ?: "Usuario"}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mantener sesión iniciada")
                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = keepLoggedIn.value,
                    onCheckedChange = {
                        keepLoggedIn.value = it
                        SessionManager.setKeepLoggedIn(context, it)
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    SessionManager.setKeepLoggedIn(context, false)
                    auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}