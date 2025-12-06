package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.models.Cita
import com.example.rehabook.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasListScreen(auth: FirebaseAuth, database: DatabaseReference, navController: NavController) {
    val user = auth.currentUser
    val rolUsuario = remember { mutableStateOf(2) }
    var citas by remember { mutableStateOf(listOf<Cita>()) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedCitaId by remember { mutableStateOf<String?>(null) }

    // ---- Leer rol del usuario ----
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            Log.d("CitasListScreen", "Leyendo rol del usuario: $uid")
            database.child("usuario").child(uid)
                .get()
                .addOnSuccessListener { snap ->
                    val usuario = snap.getValue(Usuario::class.java)
                    rolUsuario.value = usuario?.rol ?: 2
                    Log.d("CitasListScreen", "Rol del usuario: ${rolUsuario.value}")
                }
                .addOnFailureListener { e ->
                    Log.e("CitasListScreen", "Error leyendo rol de usuario: ${e.message}", e)
                }
        } ?: Log.e("CitasListScreen", "user.uid es null")
    }

    // ---- Leer citas ----
    LaunchedEffect(Unit) {
        Log.d("CitasListScreen", "Iniciando listener de citas")
        database.child("cita").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("CitasListScreen", "Snapshot recibido con ${snapshot.childrenCount} elementos")

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d("CitasListScreen", "No hay citas en la base de datos.")
                    citas = emptyList()
                    return
                }

                val lista = mutableListOf<Cita>()
                snapshot.children.forEach { snap ->
                    try {
                        // Verificamos que cada nodo de 'cita' sea válido antes de intentar convertirlo a 'Cita'
                        val cita = snap.getValue(Cita::class.java)
                        if (cita != null) {
                            // Si el ID está vacío, usamos la key de Firebase
                            if (cita.id.isBlank()) cita.id = snap.key ?: ""
                            lista.add(cita)
                            Log.d("CitasListScreen", "Cita cargada: $cita")
                        } else {
                            Log.e("CitasListScreen", "Cita nula detectada en snapshot: ${snap.key}")
                        }
                    } catch (e: Exception) {
                        Log.e("CitasListScreen", "Error convirtiendo snapshot a Cita: ${snap.key}", e)
                    }
                }
                Log.d("CitasListScreen", "Total citas cargadas: ${lista.size}")
                citas = lista
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CitasListScreen", "Error leyendo citas: ${error.message}")
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listado de Citas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        },
        floatingActionButton = {
            if (rolUsuario.value == 1) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CitasForm.route + "?id=null") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir cita")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(citas) { cita ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (rolUsuario.value == 1) {
                                navController.navigate(Screen.CitasForm.route + "?id=${cita.id}")
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Paciente: ${cita.paciente}", style = MaterialTheme.typography.titleMedium)
                        Text("Motivo: ${cita.motivo}")
                        Text("Fisioterapeuta: ${cita.fisioterapeuta}")
                        Text("Creación: ${cita.fechaCreacion}")
                        if (cita.fechaModificacion.isNotEmpty())
                            Text("Última modificación: ${cita.fechaModificacion}")

                        if (rolUsuario.value == 1) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    navController.navigate(Screen.CitasForm.route + "?id=${cita.id}")
                                }) { Text("Editar") }

                                TextButton(onClick = {
                                    // Mostrar el diálogo de confirmación de eliminación
                                    selectedCitaId = cita.id
                                    showDialog = true
                                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirmación") },
                text = { Text("¿Estás seguro de que deseas eliminar esta cita?") },
                confirmButton = {
                    TextButton(onClick = {
                        if (selectedCitaId != null) {
                            database.child("cita").child(selectedCitaId!!).removeValue()
                        }
                        showDialog = false
                    }) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

