package com.example.rehabook

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{otherUserId}")
}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    // Se inicializa UNA SOLA VEZ con la URL correcta
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        // URL CORRECTA para la región europe-west1
        database = Firebase.database("https://rehabook-default-rtdb.europe-west1.firebasedatabase.app").reference

        // Si el usuario NO desea mantener la sesión → cerrar al abrir la app
        if (!SessionManager.getKeepLoggedIn(this)) {
            auth.signOut()
        }

        setContent {
            RehabookTheme {
                // Se pasa la referencia de la base de datos a la navegación
                AppNavigation(auth, database)
            }
        }
    }
}