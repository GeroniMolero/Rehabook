package com.example.rehabook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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

sealed class Screen(val route: String){
    object Login: Screen("login")
    object Register: Screen("register")
    object Home: Screen("home")
}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        database = Firebase.database.reference

        enableEdgeToEdge()

        setContent {
            RehabookTheme {
                // Navegación centralizada
                AppNavigation(auth, database)
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RehabookTheme {
        Greeting("Android")
    }
}


@Composable
fun pantallaInicio(modifier: Modifier = Modifier){
    Image(
        painter = painterResource(id = R.drawable.pajaroestesi),
        contentDescription = "logo pajarito",
        modifier = Modifier,
        alignment= Alignment.Center,
        contentScale = ContentScale.Fit,
    )
}

@Preview
@Composable
fun pantallaInicioPreview(){
    pantallaInicio()
}