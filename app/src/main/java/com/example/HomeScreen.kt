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
import kotlinx.coroutines.withContext

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
    var isUploadingLogs by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    fun uploadLogs(context: android.content.Context) {
        if (isUploadingLogs) return
        isUploadingLogs = true
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Dumping logcat and uploading logs...", duration = SnackbarDuration.Short)
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                FileLogger.dumpLogcat(context)
                val logFile = FileLogger.getLogFile(context)
                
                if (logFile == null || !logFile.exists()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isUploadingLogs = false
                        snackbarHostState.showSnackbar("No log file found.")
                    }
                    return@withContext
                }

                try {
                    val boundary = "Boundary-" + System.currentTimeMillis()
                    val url = java.net.URL("https://tmpfiles.org/api/v1/upload")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    
                    connection.outputStream.use { os ->
                        val writer = java.io.PrintWriter(java.io.OutputStreamWriter(os, "UTF-8"), true)
                        writer.append("--$boundary\r\n")
                        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"${logFile.name}\"\r\n")
                        writer.append("Content-Type: text/plain\r\n\r\n")
                        writer.flush()
                        
                        logFile.inputStream().use { input ->
                            input.copyTo(os)
                        }
                        os.flush()
                        
                        writer.append("\r\n")
                        writer.append("--$boundary--\r\n")
                        writer.flush()
                    }
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
                        val match = urlRegex.find(response)
                        if (match != null) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isUploadingLogs = false
                                snackbarHostState.showSnackbar(
                                    message = "Uploaded: ${match.groupValues[1]}",
                                    duration = SnackbarDuration.Indefinite,
                                    actionLabel = "Dismiss"
                                )
                            }
                        } else {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isUploadingLogs = false
                                snackbarHostState.showSnackbar("Upload succeeded but URL not found.")
                            }
                        }
                    } else {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isUploadingLogs = false
                            snackbarHostState.showSnackbar("Upload failed: HTTP \$responseCode")
                        }
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isUploadingLogs = false
                        snackbarHostState.showSnackbar("Upload error: \${e.message}")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTournaments()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tournaments") },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(onClick = { uploadLogs(context) }, enabled = !isUploadingLogs) {
                        Text(if (isUploadingLogs) "Uploading..." else "Dump Logs")
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
