package com.example.rehabook.models

data class Mensaje(
    var id: String = "",
    var remitenteUid: String = "",
    var texto: String = "",
    var timestamp: Long = 0L
) {
    // Función para convertir a Map compatible con Firebase

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "remitenteUid" to remitenteUid,
            "texto" to texto,
            "timestamp" to timestamp
        )
    }
}