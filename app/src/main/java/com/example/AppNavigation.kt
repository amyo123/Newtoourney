package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = LoginRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = { navController.navigate(HomeRoute) { popUpTo(LoginRoute) { inclusive = true } } }
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToTournament = { id -> navController.navigate(TournamentDetailRoute(id)) }
            )
        }
        composable<TournamentDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TournamentDetailRoute>()
            TournamentDetailScreen(
                tournamentId = route.tournamentId,
                onNavigateToStream = { channel, isBroadcaster -> navController.navigate(StreamRoute(channel, isBroadcaster)) },
                onNavigateToChat = { id -> navController.navigate(ChatRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<StreamRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StreamRoute>()
            StreamScreen(
                channelName = route.channelName,
                isBroadcaster = route.isBroadcaster,
                onBack = { navController.popBackStack() }
            )
        }
        composable<ChatRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChatRoute>()
            ChatScreen(
                tournamentId = route.tournamentId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
