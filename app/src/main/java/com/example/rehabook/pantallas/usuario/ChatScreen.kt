package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.models.Mensaje
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    auth: FirebaseAuth,
    database: DatabaseReference, // <-- Recibe la referencia como parámetro
    navController: NavController,
    otherUserId: String
) {
    val currentUser = auth.currentUser
    val currentUserId = currentUser?.uid ?: return

    // NO se inicializa aquí, se usa la que llega por parámetro
    // val database = Firebase.database(...).reference  <-- ¡ELIMINAR ESTA LÍNEA!

    val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
    val chatRef = database.child("chats").child(chatId)

    Log.d("ChatScreen", "Iniciando chat con ID: $chatId entre $currentUserId y $otherUserId")

    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatId) {
        Log.d("ChatScreen", "Configurando listener para chatId: $chatId")
        val mensajesRef = chatRef.child("mensajes")
        mensajesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val mensaje = snapshot.getValue(Mensaje::class.java)
                mensaje?.let {
                    Log.d("ChatScreen", "Nuevo mensaje recibido: ${it.texto}")
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
                Log.e("ChatScreen", "Error escuchando mensajes: ${error.message}")
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
                    if (messageText.isNotBlank()) {
                        val mensajeKey = chatRef.child("mensajes").push().key
                        val nuevoMensaje = Mensaje(
                            id = mensajeKey ?: "",
                            remitenteUid = currentUserId,
                            texto = messageText.trim(),
                            timestamp = System.currentTimeMillis()
                        )

                        chatRef.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                chatRef.child("mensajes").child(mensajeKey!!).setValue(nuevoMensaje)
                                    .addOnSuccessListener {
                                        Log.d("ChatScreen", "Mensaje enviado con éxito.")
                                        messageText = ""
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChatScreen", "Error al enviar mensaje", e)
                                    }
                            } else {
                                val chatData = mapOf<String, Any>(
                                    "participantes/$currentUserId" to true,
                                    "participantes/$otherUserId" to true,
                                    "mensajes/$mensajeKey" to nuevoMensaje.toMap()
                                )

                                chatRef.setValue(chatData)
                                    .addOnSuccessListener {
                                        Log.d("ChatScreen", "Chat y mensaje creados con éxito.")
                                        messageText = ""
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChatScreen", "Error al crear chat y mensaje", e)
                                    }
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}