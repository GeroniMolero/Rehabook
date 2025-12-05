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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    auth: FirebaseAuth,
    database: DatabaseReference,
    navController: NavController
) {
    val adminUid = auth.currentUser?.uid ?: return
    var chats by remember { mutableStateOf(listOf<Pair<String, Usuario>>()) } // (chatId, usuario)

    LaunchedEffect(adminUid) {
        // Escucha los nodos de chat que involucren al administrador
        database.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                // Extrae el UID del otro participante
                val otherUserId = chatId.split("_").find { it != adminUid } ?: return

                // Busca los datos de ese usuario
                database.child("usuario").child(otherUserId).get()
                    .addOnSuccessListener { userSnap ->
                        val usuario = userSnap.getValue(Usuario::class.java)
                        if (usuario != null) {
                            // Evita duplicados
                            if (chats.none { it.first == chatId }) {
                                chats = chats + Pair(chatId, usuario)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatListScreen", "Error al obtener datos del usuario $otherUserId", e)
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                chats = chats.filter { it.first != chatId }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatListScreen", "Error escuchando chats: ${error.message}")
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
                    // Obtenemos el UID del otro usuario directamente desde el chatId
                    val otherUserId = chatId.split("_").find { it != adminUid }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Usamos el otherUserId que acabamos de calcular
                                if (otherUserId != null) {
                                    navController.navigate(Screen.Chat.route.replace("{otherUserId}", otherUserId))
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = usuario.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Email: ${usuario.email}")
                        }
                    }
                }
            }
        }
    }
}