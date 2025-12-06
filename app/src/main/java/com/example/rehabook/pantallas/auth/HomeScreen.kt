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
    database: DatabaseReference,
    navController: NavController
) {
    val context = LocalContext.current
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    val keepLoggedIn = remember { mutableStateOf(SessionManager.getKeepLoggedIn(context)) }

    // --- ESTADOS PARA EL CHAT ---
    var rolUsuario by remember { mutableStateOf(2) } // Por defecto, usuario normal
    var adminUid by remember { mutableStateOf<String?>(null) }
    var cargandoAdmin by remember { mutableStateOf(false) }

    val TAG = "HomeScreenDebug"

    // --- EFECTO PARA OBTENER ROL Y ADMIN UID ---
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            Log.d(TAG, "LaunchedEffect disparado para UID: $uid")
            // 1. Obtener el rol del usuario actual
            database.child("usuario").child(uid).get()
                .addOnSuccessListener { snap ->
                    rolUsuario = snap.child("rol").getValue(Int::class.java) ?: 2
                    Log.d(TAG, "Rol del usuario obtenido de BD: $rolUsuario para UID: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al obtener rol del usuario para UID: $uid", e)
                }

            // 2. Si el usuario es normal, buscar el UID del administrador
            if (rolUsuario == 2) {
                Log.d(TAG, "Usuario es rol 2, buscando administrador...")
                cargandoAdmin = true
                val adminUidTemporal = "7cT0g6mjOQN4kjHVrXmp2rO9VzE2"

                database.child("usuario").child(adminUidTemporal).child("rol").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.getValue(Int::class.java) != 1) {
                            adminUid = null
                            Log.w(TAG, "El UID proporcionado ya no es administrador: $adminUidTemporal")
                        } else {
                            adminUid = adminUidTemporal
                            Log.d(TAG, "UID del administrador encontrado y verificado: $adminUidTemporal")
                        }
                        cargandoAdmin = false
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error verificando administrador", e)
                        // Si hay un error de permisos, usamos el UID conocido directamente
                        adminUid = adminUidTemporal
                        Log.w(TAG, "Error de permisos, usando UID de administrador conocido: $adminUidTemporal")
                        cargandoAdmin = false
                    }
            } else {
                Log.d(TAG, "Usuario es rol 1 (administrador). No se busca otro admin.")
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
                        Log.d(TAG, "Botón de Soporte pulsado. rolUsuario: $rolUsuario, adminUid: $adminUid, cargandoAdmin: $cargandoAdmin")
                        if (rolUsuario == 1) {
                            Log.d(TAG, "Navegando a ChatList (es administrador)")
                            navController.navigate(Screen.ChatList.route)
                        } else {
                            if (cargandoAdmin) {
                                Toast.makeText(context, "Buscando administrador...", Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "Mostrando toast 'Buscando administrador...'")
                            } else {
                                adminUid?.let { uid ->
                                    Log.d(TAG, "Navegando a Chat con UID de admin: $uid")
                                    navController.navigate(Screen.Chat.route.replace("{otherUserId}", uid))
                                } ?: run {
                                    Log.e(TAG, "Error: adminUid es nulo al intentar navegar al chat.")
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