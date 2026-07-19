package com.example

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@com.google.firebase.firestore.IgnoreExtraProperties
data class Tournament(
    val id: String = "",
    val name: String = "",
    val game: String = "",
    val status: String = "Open",
    val prize: String = "",
    val time: String = "",
    val maxPlayers: String = "",
    val thumbnail: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTournament: (String) -> Unit,
    onNavigateToMockup: () -> Unit
) {
    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isUploadingLogs by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
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

    val darkBackground = Color(0xFF0F0C29)
    val accentNeon = Color(0xFF00FF87)
    val cardBg = Color(0xFF1A1A2E)

    Scaffold(
        containerColor = darkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("eSports Tournaments", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(onClick = { uploadLogs(context) }, enabled = !isUploadingLogs) {
                        Text(if (isUploadingLogs) "Uploading..." else "Dump Logs", color = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = darkBackground,
                contentColor = accentNeon,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentNeon, unselectedIconColor = Color.Gray,
                        selectedTextColor = accentNeon, unselectedTextColor = Color.Gray, indicatorColor = cardBg
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Discover") },
                    label = { Text("Discover") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Notifications") },
                    label = { Text("Notifications") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = accentNeon) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament", tint = darkBackground)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentNeon)
                    } else if (error != null) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { loadTournaments() }, colors = ButtonDefaults.buttonColors(containerColor = accentNeon)) {
                                Text("Retry", color = darkBackground)
                            }
                        }
                    } else if (tournaments.isEmpty()) {
                        Text("No tournaments available.", color = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("Trending Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(tournaments) { t ->
                                val isLive = t.status.equals("Live", ignoreCase = true)
                                val isFinished = t.status.equals("Finished", ignoreCase = true)
                                val gradient = when (t.game.lowercase()) {
                                    "pubg" -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
                                    "valorant" -> listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                                    "codm" -> listOf(Color(0xFF00B4DB), Color(0xFF0083B0))
                                    "free fire" -> listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                                    else -> listOf(Color(0xFF4CA1AF), Color(0xFFC4E0E5))
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToTournament(t.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Column {
                                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                            if (t.thumbnail != null) {
                                                AsyncImage(
                                                    model = t.thumbnail,
                                                    contentDescription = t.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(gradient)))
                                            }
                                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                                            
                                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                                if (isLive) {
                                                    Surface(color = Color.Red.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                                        Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                } else if (isFinished) {
                                                    Surface(color = Color.DarkGray.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                                        Text("FINISHED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                } else {
                                                    Surface(color = Color.Gray.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                                        Text("UPCOMING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                                Text(t.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                            }
                                        }
                                        
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("${t.game} | Time: ${t.time} | 0/${t.maxPlayers} Players", color = Color.LightGray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.List, contentDescription = "Discover", tint = accentNeon, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Discover New Tournaments", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Coming soon...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Email, contentDescription = "Notifications", tint = accentNeon, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Your Notifications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("No new notifications.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                3 -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val sharedPrefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                    var profileUrl by remember { mutableStateOf(sharedPrefs.getString("profile_url", null)) }
                    var isUploadingProfile by remember { mutableStateOf(false) }

                    val profileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        if (uri != null) {
                            isUploadingProfile = true
                            coroutineScope.launch {
                                val uploadedUrl = CloudinaryHelper.uploadImage(context, uri)
                                if (uploadedUrl != null) {
                                    sharedPrefs.edit().putString("profile_url", uploadedUrl).apply()
                                    profileUrl = uploadedUrl
                                } else {
                                    snackbarHostState.showSnackbar("Failed to upload profile picture")
                                }
                                isUploadingProfile = false
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8A2387))
                                .clickable { profileLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileUrl != null) {
                                AsyncImage(
                                    model = profileUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Add Profile Picture", tint = Color.White, modifier = Modifier.size(64.dp))
                            }
                            if (isUploadingProfile) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = accentNeon)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Player One", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Pro Gamer", color = accentNeon, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap image to change", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
        
        var name by remember { mutableStateOf("") }
        var game by remember { mutableStateOf("") }
        var prize by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var maxPlayers by remember { mutableStateOf("") }
        var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
        var isUploadingThumbnail by remember { mutableStateOf(false) }

        val thumbnailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            thumbnailUri = uri
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = cardBg,
                title = { Text("Create Tournament", color = Color.White) },
                text = {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                                .clickable { thumbnailLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumbnailUri != null) {
                                AsyncImage(
                                    model = thumbnailUri,
                                    contentDescription = "Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Thumbnail", tint = Color.LightGray)
                                    Text("Add Thumbnail", color = Color.LightGray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name, 
                            onValueChange = { name = it }, 
                            label = { Text("Name", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentNeon,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = game, 
                            onValueChange = { game = it }, 
                            label = { Text("Game", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentNeon,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = prize, 
                            onValueChange = { prize = it }, 
                            label = { Text("Prize (Optional)", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentNeon,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = time, 
                            onValueChange = { time = it }, 
                            label = { Text("Time", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentNeon,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = maxPlayers, 
                            onValueChange = { maxPlayers = it }, 
                            label = { Text("Max Players", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentNeon,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                isUploadingThumbnail = true
                                var thumbnailUrl: String? = null
                                if (thumbnailUri != null) {
                                    thumbnailUrl = CloudinaryHelper.uploadImage(context, thumbnailUri!!)
                                }
                                val db = FirebaseFirestore.getInstance()
                                val ref = db.collection("tournaments").document()
                                ref.set(Tournament(id = ref.id, name = name, game = game, status = "Upcoming", prize = prize, time = time, maxPlayers = maxPlayers, thumbnail = thumbnailUrl)).await()
                                showCreateDialog = false
                                thumbnailUri = null
                                loadTournaments()
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isUploadingThumbnail = false
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = accentNeon), enabled = !isUploadingThumbnail) {
                        Text(if (isUploadingThumbnail) "Creating..." else "Create", color = darkBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}
