package com.example.rehabook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rehabook.Screen
import com.example.rehabook.pantallas.auth.HomeScreen
import com.example.rehabook.pantallas.auth.LoginScreen
import com.example.rehabook.pantallas.auth.RegisterScreen
import com.example.rehabook.pantallas.usuario.CitasFormScreen
import com.example.rehabook.pantallas.usuario.CitasListScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

@Composable
fun AppNavigation(auth: FirebaseAuth, database: DatabaseReference) {
    val navController = rememberNavController()

    val startDestination =
        if (auth.currentUser != null) Screen.Home.route
        else Screen.Login.route

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

        composable(Screen.CitasList.route) {
            CitasListScreen(auth, database, navController)
        }

        composable(
            route = Screen.CitasForm.route + "?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CitasFormScreen(auth, database, navController)
        }
    }
}
