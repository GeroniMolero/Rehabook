package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.rehabook.utils.ChatManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    auth: FirebaseAuth,
    database: DatabaseReference,
    navController: NavController
) {
    val adminUid = auth.currentUser?.uid ?: return
    var chats by remember { mutableStateOf(listOf<Pair<String, Usuario>>()) }
    val tag = "ChatListDebug"

    var showDialog by remember { mutableStateOf(false) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }

    // Crear una instancia de ChatManager
    val chatManager = remember { ChatManager(database) }

    LaunchedEffect(adminUid) {
        Log.d(tag, "LaunchedEffect en ChatListScreen. Configurando listener de chats para admin: $adminUid")
        database.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                Log.d(tag, "Nuevo chat detectado: $chatId")
                // Extrae el UID del otro participante
                val otherUserId = chatId.split("_").find { it != adminUid }
                Log.d(tag, "ChatId: $chatId, AdminUid: $adminUid, OtherUserId: $otherUserId")

                if (otherUserId != null) {
                    // Busca los datos de ese usuario
                    database.child("usuario").child(otherUserId).get()
                        .addOnSuccessListener { userSnap ->
                            val usuario = userSnap.getValue(Usuario::class.java)
                            if (usuario != null) {
                                Log.d(tag, "Usuario encontrado para chat $chatId: ${usuario.nombre}")
                                // Evita duplicados
                                if (chats.none { it.first == chatId }) {
                                    chats = chats + Pair(chatId, usuario)
                                    Log.d(tag, "Chat añadido a la lista. Total chats: ${chats.size}")
                                } else {
                                    Log.w(tag, "Chat $chatId ya existía en la lista.")
                                }
                            } else {
                                Log.w(tag, "No se pudo deserializar el usuario para UID: $otherUserId")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error al obtener datos del usuario $otherUserId", e)
                        }
                } else {
                    Log.w(tag, "No se pudo extraer otherUserId de chatId: $chatId")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                Log.d(tag, "Chat eliminado: $chatId")
                chats = chats.filter { it.first != chatId }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Error escuchando chats: ${error.message}", error.toException())
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats de Soporte") },
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
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay chats activos.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(chats) { (chatId, usuario) ->
                    val otherUserId = chatId.split("_").find { it != adminUid }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (otherUserId != null) {
                                    navController.navigate(Screen.Chat.route.replace("{otherUserId}", otherUserId))
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = usuario.nombre,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Email: ${usuario.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        // Mostrar diálogo de confirmación antes de vaciar el chat
                                        selectedChatId = chatId
                                        showDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Vaciar chat",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Vaciar chat", style = MaterialTheme.typography.bodyMedium)
                                }
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
                text = { Text("¿Estás seguro de que deseas vaciar este chat?") },
                confirmButton = {
                    TextButton(onClick = {
                        if (selectedChatId != null) {
                            chatManager.vaciarChat(selectedChatId!!)
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
