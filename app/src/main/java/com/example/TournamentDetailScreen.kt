package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onNavigateToStream: (String, Boolean) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onBack: () -> Unit
) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tournamentId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("tournaments").document(tournamentId).get().await()
            tournament = doc.toObject(Tournament::class.java)
            if (tournament == null) error = "Tournament not found"
        } catch (e: Exception) {
            error = e.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tournament?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            } else if (tournament != null) {
                Text("Game: ${tournament!!.game}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onNavigateToStream(tournament!!.id, true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Live Stream (Host)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onNavigateToStream(tournament!!.id, false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Watch Live Stream (Audience)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onNavigateToChat(tournament!!.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tournament Chat")
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
