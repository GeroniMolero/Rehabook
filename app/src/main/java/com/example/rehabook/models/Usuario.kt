package com.example.rehabook.models

data class Usuario(
    var nombre: String  = "",
    var email: String = "",
    var telefono: String = "",
    var dni: String = "",
    var rol: Int = 2
)
