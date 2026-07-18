package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@com.google.firebase.firestore.IgnoreExtraProperties
data class Tournament(
    val id: String = "",
    val name: String = "",
    val game: String = "",
    val status: String = "Open"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTournament: (String) -> Unit
) {
    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    fun loadTournaments() {
        isLoading = true
        coroutineScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("tournaments").get().await()
                tournaments = snapshot.toObjects(Tournament::class.java)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Failed to load tournaments. (Is google-services.json configured?)"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTournaments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournaments") },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(onClick = { FileLogger.dumpLogcat(context) }) {
                        Text("Dump Logs")
                    }
                    TextButton(onClick = { throw RuntimeException("Manual Test Crash triggered by user!") }) {
                        Text("Test Crash", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { loadTournaments() }) {
                        Text("Retry")
                    }
                }
            } else if (tournaments.isEmpty()) {
                Text("No tournaments available.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(tournaments) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onNavigateToTournament(t.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(t.name, style = MaterialTheme.typography.titleMedium)
                                Text(t.game, style = MaterialTheme.typography.bodyMedium)
                                Text(t.status, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            var game by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Tournament") },
                text = {
                    Column {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                        OutlinedTextField(value = game, onValueChange = { game = it }, label = { Text("Game") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val ref = db.collection("tournaments").document()
                                ref.set(Tournament(id = ref.id, name = name, game = game)).await()
                                showCreateDialog = false
                                loadTournaments()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
