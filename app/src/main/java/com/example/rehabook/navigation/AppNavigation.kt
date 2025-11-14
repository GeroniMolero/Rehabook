package com.example.rehabook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rehabook.Screen
import com.example.rehabook.auth.pantallas.HomeScreen
import com.example.rehabook.auth.pantallas.LoginScreen
import com.example.rehabook.auth.pantallas.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

@Composable
fun AppNavigation(auth: FirebaseAuth, database: DatabaseReference) {
    val navController = rememberNavController()

    // Determinar pantalla inicial según autenticación
    val startDestination = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(auth, navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(auth, database, navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(auth, navController)
        }
    }
}

