package com.example.rehabook.pantallas.usuario

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rehabook.Screen
import com.example.rehabook.models.ChatPreview
import com.example.rehabook.models.Usuario
import com.example.rehabook.utils.ChatManager
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

    var chats by remember { mutableStateOf(listOf<ChatPreview>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf("recientes") }
    var searchQuery by remember { mutableStateOf("") } // Estado del buscador

    val chatManager = remember { ChatManager(database) }
    val TAG = "ChatListScreen"

    // ---- Escuchar todos los chats ----
    LaunchedEffect(adminUid) {
        database.child("chats").addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatId = snapshot.key ?: return
                val participants = chatId.split("_")
                val otherUserId = participants.find { it != adminUid } ?: return

                // Obtener datos del usuario
                database.child("usuario").child(otherUserId).get()
                    .addOnSuccessListener { userSnap ->
                        val usuario = userSnap.getValue(Usuario::class.java) ?: return@addOnSuccessListener

                        // Evitar duplicados
                        if (chats.any { it.chatId == chatId }) return@addOnSuccessListener

                        // Crear entrada inicial
                        chats = chats + ChatPreview(
                            chatId = chatId,
                            usuario = usuario,
                            lastTimestamp = null,
                            lastReadAdmin = null
                        )

                        // Escuchar últimos mensajes
                        escucharUltimoMensaje(database, chatId) { timestamp ->
                            chats = chats.map {
                                if (it.chatId == chatId) it.copy(lastTimestamp = timestamp)
                                else it
                            }
                        }

                        // Escuchar lastRead_admin
                        escucharUltimaLectura(database, chatId) { lastRead ->
                            chats = chats.map {
                                if (it.chatId == chatId) it.copy(lastReadAdmin = lastRead)
                                else it
                            }
                        }
                    }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chatId = snapshot.key ?: return
                chats = chats.filter { it.chatId != chatId }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---- Ordenar chats ----
    val sortedChats = when (sortOption) {
        "recientes" -> chats.sortedByDescending { it.lastTimestamp ?: 0L }
        "antiguos" -> chats.sortedBy { it.lastTimestamp ?: 0L }
        else -> chats
    }

    // ---- Filtrar chats según buscador ----
    val filteredChats = sortedChats.filter { chat ->
        val query = searchQuery.lowercase()
        chat.usuario.nombre.lowercase().contains(query) ||
                chat.usuario.email.lowercase().contains(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats de tu consulta") },
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
        Column(Modifier.padding(padding)) {

            //Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar usuario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            //Selector de orden
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                var expanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(
                            when (sortOption) {
                                "recientes" -> "Más recientes"
                                "antiguos" -> "Más antiguos"
                                else -> "Sin ordenar"
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Más recientes") },
                            onClick = {
                                sortOption = "recientes"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Más antiguos") },
                            onClick = {
                                sortOption = "antiguos"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sin ordenar") },
                            onClick = {
                                sortOption = "normal"
                                expanded = false
                            }
                        )
                    }
                }
            }

            //Lista de chats
            if (filteredChats.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay chats activos.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChats) { chat ->

                        val unread = chat.lastTimestamp != null &&
                                (chat.lastReadAdmin == null || chat.lastTimestamp!! > chat.lastReadAdmin!!)

                        val cardColor =
                            if (unread) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val otherUserId = chat.chatId.split("_").find { it != adminUid }
                                    if (otherUserId != null) {
                                        navController.navigate(
                                            Screen.Chat.route.replace("{otherUserId}", otherUserId)
                                        )
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp) // Mejorar espaciado horizontal
                                .shadow(4.dp, shape = MaterialTheme.shapes.medium) // Agregar sombra para resaltar la tarjeta
                                .clip(MaterialTheme.shapes.medium) // Bordes redondeados
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Espaciado consistente entre los elementos
                            ) {
                                // Nombre del usuario: Más grande y en negrita
                                Text(
                                    text = chat.usuario.nombre,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold, // Negrita
                                        color = MaterialTheme.colorScheme.primary // Color destacado
                                    )
                                )

                                // Email: Más pequeño y con color gris
                                Text(
                                    text = "Email: ${chat.usuario.email}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant // Gris más suave
                                    )
                                )

                                // Último mensaje: Condicional si existe
                                if (chat.lastTimestamp != null) {
                                    Text(
                                        "Último mensaje: ${formatTimestamp(chat.lastTimestamp!!)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant // Gris más suave
                                        )
                                    )
                                }

                                Spacer(Modifier.height(8.dp)) // Espaciado entre los elementos

                                // Botón de "Vaciar chat"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedChatId = chat.chatId
                                            showDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error // Color rojo para indicar acción de eliminación
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Vaciar")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Vaciar chat")
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        // ---- DIÁLOGO DE CONFIRMACIÓN ----
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirmación") },
                text = { Text("¿Estás seguro de que deseas vaciar este chat?") },
                confirmButton = {
                    TextButton(onClick = {
                        selectedChatId?.let { chatManager.vaciarChat(it) }
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


/* ---------------------------------------------------------
   FUNCIONES AUXILIARES
--------------------------------------------------------- */

// Escucha el último timestamp del chat
fun escucharUltimoMensaje(
    database: DatabaseReference,
    chatId: String,
    onLastTimestamp: (Long?) -> Unit
) {
    database.child("chats").child(chatId).child("mensajes")
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var last: Long? = null
                snapshot.children.forEach { msg ->
                    val ts = msg.child("timestamp").getValue(Long::class.java)
                    if (ts != null && (last == null || ts > last!!)) {
                        last = ts
                    }
                }
                onLastTimestamp(last)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
}

// Escucha cuándo el admin leyó el chat
fun escucharUltimaLectura(
    database: DatabaseReference,
    chatId: String,
    onLastRead: (Long?) -> Unit
) {
    database.child("chats").child(chatId).child("lastRead_admin")
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onLastRead(snapshot.getValue(Long::class.java))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
}

// Formato fecha
fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
    return sdf.format(java.util.Date(timestamp))
}
