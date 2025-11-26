import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import com.example.rehabook.models.Cita

fun limpiarYCrearCitaPrueba(database: DatabaseReference) {
    val citasRef = database.child("cita")
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    citasRef.get().addOnSuccessListener { snapshot ->
        var tieneCitaValida = false

        snapshot.children.forEach { child ->
            val citaValida = try {
                child.getValue(Cita::class.java)
            } catch (e: Exception) {
                null
            }

            if (citaValida == null) {
                Log.d("FirebaseCleanup", "Eliminando hijo corrupto: ${child.key}")
                citasRef.child(child.key!!).removeValue()
            } else {
                tieneCitaValida = true
            }
        }

        // Crear solo un hijo de prueba si no existe ninguno válido
        if (!tieneCitaValida) {
            Log.d("FirebaseCleanup", "No hay citas válidas. Creando cita de prueba.")
            val key = citasRef.push().key!!
            val citaPrueba = Cita(
                id = key,
                paciente = "Paciente de prueba",
                motivo = "Motivo de prueba",
                diagnostico = "Diagnóstico de prueba",
                fisioterapeuta = "Fisioterapeuta de prueba",
                fechaCreacion = sdf.format(Date()),
                fechaModificacion = sdf.format(Date())
            )
            citasRef.child(key).setValue(citaPrueba)
        }

    }.addOnFailureListener { e ->
        Log.e("FirebaseCleanup", "Error al leer nodo cita: ${e.message}", e)
    }
}
