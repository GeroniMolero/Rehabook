package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.models.Mensaje
import com.example.rehabook.utils.formatTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    auth: FirebaseAuth,
    database: DatabaseReference,
    navController: NavController,
    otherUserId: String
) {
    val currentUser = auth.currentUser ?: return
    val currentUserId = currentUser.uid
    val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
    val chatRef = database.child("chats").child(chatId)
    val TAG = "ChatScreenDebug"

    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var rolUsuario by remember { mutableStateOf(2) }

    // ---- Obtener rol del usuario ----
    LaunchedEffect(currentUserId) {
        try {
            database.child("usuario").child(currentUserId).get()
                .addOnSuccessListener { snap ->
                    rolUsuario = snap.child("rol").getValue(Int::class.java) ?: 2
                    Log.d(TAG, "Rol del usuario: $rolUsuario")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error obteniendo rol", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al obtener rol", e)
        }
    }

    // ---- Escuchar mensajes en tiempo real ----
    LaunchedEffect(chatId) {
        try {
            chatRef.child("mensajes").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    try {
                        val mensaje = snapshot.getValue(Mensaje::class.java)
                        mensaje?.let {
                            mensajes = mensajes + it
                            coroutineScope.launch {
                                delay(100)
                                if (mensajes.isNotEmpty())
                                    listState.animateScrollToItem(mensajes.size - 1)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en onChildAdded", e)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Listener cancelado: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Excepción Listener mensajes", e)
        }
    }

    // ---- Marcar mensajes como leídos automáticamente ----
    LaunchedEffect(mensajes) {
        try {
            val lastTimestamp = mensajes.maxOfOrNull { it.timestamp }
            lastTimestamp?.let {
                chatRef.child("lastRead_${if (rolUsuario == 1) "user" else "admin"}")
                    .setValue(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando mensajes como leídos", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat con ${if (rolUsuario == 1) "cliente" else "tu consulta"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ---- Lista de mensajes ----
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(mensajes) { mensaje ->
                    val isFromMe = mensaje.remitenteUid == currentUserId
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 280.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFromMe)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = mensaje.texto)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTimestamp(mensaje.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ---- Input de mensaje ----
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") }
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    try {
                        if (messageText.isNotBlank()) {
                            val mensajeKey = chatRef.child("mensajes").push().key ?: return@IconButton
                            val nuevoMensaje = Mensaje(
                                id = mensajeKey,
                                remitenteUid = currentUserId,
                                texto = messageText.trim(),
                                timestamp = System.currentTimeMillis()
                            )

                            // Crear o añadir mensaje
                            chatRef.get().addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    chatRef.child("mensajes").child(mensajeKey).setValue(nuevoMensaje)
                                } else {
                                    val chatData = hashMapOf<String, Any>(
                                        "participantes" to hashMapOf(
                                            currentUserId to true,
                                            otherUserId to true
                                        ),
                                        "mensajes" to hashMapOf(mensajeKey to nuevoMensaje.toMap())
                                    )
                                    chatRef.setValue(chatData)
                                }
                                messageText = ""
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enviando mensaje", e)
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}
