package com.example.rehabook.models

data class Cita (
    var paciente: String = "",
    var fechaCreacion: String = "",
    var fechaModificacion:String = "",
    var motivo: String  = "",
    var diagnostico: String = "",
    var fisioterapeuta: String = ""
)