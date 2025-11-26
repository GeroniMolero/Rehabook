package com.example.rehabook.pantallas.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.models.Cita
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.Alignment
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasFormScreen(
    auth: FirebaseAuth,
    database: DatabaseReference,
    navController: NavController
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val idCita = navController.currentBackStackEntry?.arguments?.getString("id")

    var paciente by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }
    var fisioterapeuta by remember { mutableStateOf("") }
    var fechaCreacion by remember { mutableStateOf("") }
    var fechaModificacion by remember { mutableStateOf("") }

    var cargando by remember { mutableStateOf(idCita != null && idCita != "null") }
    var mostrandoError by remember { mutableStateOf(false) }

    // --- Cargar cita si es edición ---
    LaunchedEffect(idCita) {
        if (!idCita.isNullOrEmpty() && idCita != "null") {
            database.child("cita").child(idCita).get()
                .addOnSuccessListener { snap ->
                    val cita = snap.getValue(Cita::class.java)
                    if (cita != null) {
                        paciente = cita.paciente
                        motivo = cita.motivo
                        diagnostico = cita.diagnostico
                        fisioterapeuta = cita.fisioterapeuta
                        fechaCreacion = cita.fechaCreacion
                        fechaModificacion = cita.fechaModificacion
                    } else {
                        Log.e("CitasFormScreen", "Cita no encontrada con id: $idCita")
                    }
                    cargando = false
                }
                .addOnFailureListener {
                    Log.e("CitasFormScreen", "Error cargando cita: ${it.message}")
                    cargando = false
                }
        } else {
            fechaCreacion = sdf.format(Date())
            cargando = false
        }
    }

    fun guardarCita() {
        if (paciente.isBlank() || motivo.isBlank()) {
            mostrandoError = true
            return
        }

        val fechaAhora = sdf.format(Date())
        val key = idCita ?: database.child("cita").push().key!!
        val cita = Cita(
            id = key,
            paciente = paciente,
            motivo = motivo,
            diagnostico = diagnostico,
            fisioterapeuta = fisioterapeuta,
            fechaCreacion = if (idCita == null || idCita == "null") fechaAhora else fechaCreacion,
            fechaModificacion = fechaAhora
        )

        database.child("cita").child(key).setValue(cita)
            .addOnSuccessListener { Log.d("CitasFormScreen", "Cita guardada: $key") }
            .addOnFailureListener { Log.e("CitasFormScreen", "Error guardando cita: ${it.message}") }

        navController.popBackStack()
    }

    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (idCita == null || idCita == "null") "Crear Cita" else "Editar Cita") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { guardarCita() }) {
                Icon(Icons.Default.Check, "Guardar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = paciente,
                onValueChange = { paciente = it },
                label = { Text("Paciente") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it },
                label = { Text("Motivo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = diagnostico,
                onValueChange = { diagnostico = it },
                label = { Text("Diagnóstico") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fisioterapeuta,
                onValueChange = { fisioterapeuta = it },
                label = { Text("Fisioterapeuta") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Fecha creación: $fechaCreacion")
            if (idCita != null && idCita != "null") Text("Última modificación: $fechaModificacion")

            if (mostrandoError) {
                Text(
                    "Paciente y Motivo son obligatorios",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
