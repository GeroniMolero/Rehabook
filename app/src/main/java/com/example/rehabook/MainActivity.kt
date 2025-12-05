package com.example.rehabook

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rehabook.ui.theme.RehabookTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import com.example.rehabook.navigation.AppNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.auth.auth
import com.example.rehabook.utils.SessionManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp

sealed class Screen(val route: String){
    object Login: Screen("login")
    object Register: Screen("register")
    object Home: Screen("home")
    object CitasForm : Screen("CitasForm")
    object CitasList : Screen("CitasList")

    object ChatList : Screen("chat_list") // Solo para admin

    object Chat : Screen("chat/{otherUserId}") // Pantalla de chat
}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        database = Firebase.database("https://rehabook-default-rtdb.europe-west1.firebasedatabase.app").reference

        // Si el usuario NO desea mantener la sesión → cerrar al abrir la app
        if (!SessionManager.getKeepLoggedIn(this)) {
            auth.signOut()
        }

        setContent {
            RehabookTheme {
                AppNavigation(auth, database)


                /*Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    // En MainActivity.kt, dentro del setContent
                    Button(onClick = {
                        val adminUid = "7cT0g6mjOQN4kjHVrXmp2rO9VzE2" // UID de Geronimo
                        val usuarioUid = "04Cscb0fgAdRuqoQgcIuZgFTNz22" // UID de Ale

                        val chatId = listOf(usuarioUid, adminUid).sorted().joinToString("_")
                        val mensajesRef = database.child("chats").child(chatId).child("mensajes")

                        val mensaje1 = mapOf(
                            "remitenteUid" to usuarioUid,
                            "texto" to "Hola, necesito ayuda con mi cita.",
                            "timestamp" to System.currentTimeMillis()
                        )
                        val mensaje2 = mapOf(
                            "remitenteUid" to adminUid,
                            "texto" to "Claro, Ale. ¿En qué puedo ayudarte?",
                            "timestamp" to System.currentTimeMillis()
                        )

                        mensajesRef.push().setValue(mensaje1)
                        mensajesRef.push().setValue(mensaje2)
                            .addOnSuccessListener {
                                Log.d("RehabookTest", "Chat de prueba creado exitosamente.")
                                Toast.makeText(this@MainActivity, "Chat de prueba creado!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("RehabookTest", "Error creando chat de prueba: ${e.message}", e)
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }) {
                        Text("Crear Chat de Prueba")
                    }
                }*/

                //Boton para probar la escritura en base de datos
                /*Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        val testRef = database.child("test_write")
                        testRef.setValue("Hello from Rehabook " + System.currentTimeMillis())
                            .addOnSuccessListener {
                                Log.d("RehabookTest", "TEST RTDB: Escritura de prueba exitosa.")
                                Toast.makeText(this@MainActivity, "TEST RTDB: Escritura exitosa!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("RehabookTest", "TEST RTDB: Error en escritura de prueba: ${e.message}", e)
                                Toast.makeText(this@MainActivity, "TEST RTDB: Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("RehabookTest", "TEST RTDB: addOnCompleteListener: Éxito.")
                                } else {
                                    Log.e("RehabookTest", "TEST RTDB: addOnCompleteListener: Fallo. Excepción: ${task.exception?.message}", task.exception)
                                }
                            }
                    }) {
                        Text("Probar Escritura RTDB")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }*/

            }
        }
    }
}
