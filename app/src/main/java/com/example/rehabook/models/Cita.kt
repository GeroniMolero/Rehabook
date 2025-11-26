package com.example.rehabook.models

data class Cita(
    var id: String = "",
    var paciente: String = "",
    var motivo: String = "",
    var fisioterapeuta: String = "",
    var diagnostico: String = "",
    var fechaCreacion: String = "",
    var fechaModificacion: String = ""
)
