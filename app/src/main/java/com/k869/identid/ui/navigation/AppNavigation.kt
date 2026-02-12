package com.k869.identid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k869.identid.ui.screens.wallet.WalletScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController, 
        startDestination = "wallet",
        modifier = modifier
    ) {
        composable("wallet") {
            WalletScreen()
        }
    }
}
