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
    val currentUser = auth.currentUser
    val currentUserId = currentUser?.uid ?: return
    val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
    val chatRef = database.child("chats").child(chatId)
    val TAG = "ChatScreenDebug"

    Log.d(TAG, "ChatScreen compuesto. currentUserId: $currentUserId, otherUserId: $otherUserId, chatId: $chatId")

    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var rolUsuario by remember { mutableStateOf(2) }

    LaunchedEffect(currentUserId) {
        Log.d(TAG, "LaunchedEffect para obtener rol. currentUserId: $currentUserId")
        database.child("usuario").child(currentUserId).get()
            .addOnSuccessListener { snap ->
                rolUsuario = snap.child("rol").getValue(Int::class.java) ?: 2
                Log.d(TAG, "Rol del usuario obtenido en ChatScreen: $rolUsuario")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo rol del usuario en ChatScreen", e)
            }
    }

    LaunchedEffect(chatId) {
        Log.d(TAG, "LaunchedEffect para listener de mensajes. chatId: $chatId")
        val mensajesRef = chatRef.child("mensajes")
        mensajesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val mensaje = snapshot.getValue(Mensaje::class.java)
                mensaje?.let {
                    Log.d(TAG, "Nuevo mensaje recibido: ${it.texto} de ${it.remitenteUid}")
                    mensajes = mensajes + it
                    coroutineScope.launch {
                        delay(100)
                        listState.animateScrollToItem(mensajes.size - 1)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error escuchando mensajes: ${error.message}", error.toException())
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat con Soporte") },
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
                                containerColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = mensaje.texto,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

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
                        Log.d(TAG, "Botón de enviar pulsado. messageText: '$messageText'")

                        if (messageText.isNotBlank()) {
                            val mensajeKey = chatRef.child("mensajes").push().key ?: return@IconButton
                            if (mensajeKey.contains('/') || mensajeKey.contains('.') || mensajeKey.contains('#') ||
                                mensajeKey.contains('$') || mensajeKey.contains('[') || mensajeKey.contains(']')) {
                                Log.e(TAG, "La clave del mensaje contiene caracteres no válidos: $mensajeKey")
                                return@IconButton
                            }
                            val nuevoMensaje = Mensaje(
                                id = mensajeKey,
                                remitenteUid = currentUserId,
                                texto = messageText.trim(),
                                timestamp = System.currentTimeMillis()
                            )
                            Log.d(TAG, "Mensaje a enviar: $nuevoMensaje")

                            // Verificar si el chat existe
                            chatRef.get().addOnSuccessListener { snapshot ->
                                Log.d(TAG, "Verificando si chat existe. exists: ${snapshot.exists()}")
                                if (snapshot.exists()) {
                                    Log.d(TAG, "Chat existe. Añadiendo mensaje.")
                                    // El chat existe, solo añadir el mensaje
                                    chatRef.child("mensajes").child(mensajeKey).setValue(nuevoMensaje)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Mensaje enviado con éxito.")
                                            messageText = ""
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error al enviar mensaje", e)
                                        }
                                } else {
                                    Log.d(TAG, "Chat no existe. Creando chat y primer mensaje.")
                                    // El chat no existe, crearlo con los participantes y el primer mensaje
                                    val chatData = HashMap<String, Any>()
                                    val participantesData = HashMap<String, Boolean>()
                                    participantesData[currentUserId] = true
                                    participantesData[otherUserId] = true

                                    val mensajesData = HashMap<String, Any>()
                                    mensajesData[mensajeKey] = nuevoMensaje.toMap()

                                    chatData["participantes"] = participantesData
                                    chatData["mensajes"] = mensajesData

                                    Log.d(TAG, "Datos del nuevo chat: $chatData")
                                    chatRef.setValue(chatData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Chat y mensaje creados con éxito.")
                                            messageText = ""
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error al crear chat y mensaje", e)
                                        }
                                }
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Error al verificar si el chat existe", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Excepción no controlada al enviar mensaje", e)
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}