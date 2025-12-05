package com.example.rehabook.models

data class Mensaje(
    var id: String = "",
    var remitenteUid: String = "",
    var texto: String = "",
    var timestamp: Long = 0L // Timestamp para ordenar los mensajes
)