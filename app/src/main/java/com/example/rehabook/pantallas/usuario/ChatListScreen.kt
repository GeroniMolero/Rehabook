package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var chats by remember { mutableStateOf(listOf<Pair<String, Usuario>>()) } // (chatId, usuario)
    val TAG = "ChatListDebug"

    Log.d(TAG, "ChatListScreen compuesto. Admin UID: $adminUid")

    LaunchedEffect(adminUid) {
        Log.d(TAG, "LaunchedEffect en ChatListScreen. Configurando listener de chats para admin: $adminUid")
        database.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                Log.d(TAG, "Nuevo chat detectado: $chatId")
                // Extrae el UID del otro participante
                val otherUserId = chatId.split("_").find { it != adminUid }
                Log.d(TAG, "ChatId: $chatId, AdminUid: $adminUid, OtherUserId: $otherUserId")

                if (otherUserId != null) {
                    // Busca los datos de ese usuario
                    database.child("usuario").child(otherUserId).get()
                        .addOnSuccessListener { userSnap ->
                            val usuario = userSnap.getValue(Usuario::class.java)
                            if (usuario != null) {
                                Log.d(TAG, "Usuario encontrado para chat $chatId: ${usuario.nombre}")
                                // Evita duplicados
                                if (chats.none { it.first == chatId }) {
                                    chats = chats + Pair(chatId, usuario)
                                    Log.d(TAG, "Chat añadido a la lista. Total chats: ${chats.size}")
                                } else {
                                    Log.w(TAG, "Chat $chatId ya existía en la lista.")
                                }
                            } else {
                                Log.w(TAG, "No se pudo deserializar el usuario para UID: $otherUserId")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al obtener datos del usuario $otherUserId", e)
                        }
                } else {
                    Log.w(TAG, "No se pudo extraer otherUserId de chatId: $chatId")
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                Log.d(TAG, "Chat eliminado: $chatId")
                chats = chats.filter { it.first != chatId }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error escuchando chats: ${error.message}", error.toException())
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats de Soporte") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay chats activos.")
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
                                Log.d(TAG, "Chat clickeado. ChatId: $chatId, otherUserId: $otherUserId")
                                if (otherUserId != null) {
                                    navController.navigate(Screen.Chat.route.replace("{otherUserId}", otherUserId))
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = usuario.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Email: ${usuario.email}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    Log.d(TAG, "Vaciar chat: $chatId")
                                    // Vaciar el chat eliminando los mensajes
                                    database.child("chats").child(chatId).child("mensajes").removeValue()
                                    Log.d(TAG, "Mensajes del chat $chatId eliminados.")
                                }) {
                                    Text("Vaciar chat", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}