package com.example.rehabook.utils

import android.util.Log
import com.google.firebase.database.DatabaseReference

class ChatManager(private val database: DatabaseReference) {

    // Función para vaciar un chat (eliminar los mensajes del chat)
    fun vaciarChat(chatId: String) {
        Log.d("ChatManager", "Vaciar chat: $chatId")
        // Eliminar todos los mensajes dentro del chat
        database.child("chats").child(chatId).child("mensajes").removeValue()
            .addOnSuccessListener {
                Log.d("ChatManager", "Mensajes del chat $chatId eliminados correctamente.")
            }
            .addOnFailureListener { e ->
                Log.e("ChatManager", "Error al vaciar el chat $chatId", e)
            }
    }
}
