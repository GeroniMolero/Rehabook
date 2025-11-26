package com.example.rehabook.pantallas.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.rehabook.utils.SessionManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List

@Composable
fun HomeScreen(auth: FirebaseAuth, navController: NavController) {
    val context = LocalContext.current
    val user = auth.currentUser

    val keepLoggedIn = remember { mutableStateOf(SessionManager.getKeepLoggedIn(context)) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.CitasList.route) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Citas") },
                    label = { Text("Citas") }
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
