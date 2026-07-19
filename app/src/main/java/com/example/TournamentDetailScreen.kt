package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class TournamentPlayer(
    val id: String = "",
    val name: String = "",
    val role: String = ""
)

data class TournamentMatch(
    val id: String = "",
    val team1: String = "",
    val team2: String = "",
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val team1Players: List<TournamentPlayer> = emptyList(),
    val team2Players: List<TournamentPlayer> = emptyList()
)

enum class DetailTab(val title: String) {
    SCORES("Scores"),
    CHAT("Live Chat"),
    DETAILS("Details"),
    TEAMS("Teams")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToStream: (String, Boolean) -> Unit
) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var matches by remember { mutableStateOf<List<TournamentMatch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddMatchDialog by remember { mutableStateOf(false) }
    
    var teamToEdit by remember { mutableStateOf<Triple<String, Boolean, String>?>(null) }
    var playerToAdd by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var playerToEdit by remember { mutableStateOf<Triple<String, Boolean, TournamentPlayer>?>(null) }
    
    var showStreamOptions by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    
    var selectedTab by remember { mutableStateOf(DetailTab.SCORES) }

    val darkBackground = Color(0xFF0F1115)
    val cardBg = Color(0xFF16181E)
    val goldAccent = Color(0xFFD4AF37)
    val tealAccent = Color(0xFF2DD4BF)
    val redAccent = Color(0xFFEF4444)

    fun loadTournamentData() {
        coroutineScope.launch {
            try {
                val snapshot = db.collection("tournaments").document(tournamentId).get().await()
                tournament = snapshot.toObject(Tournament::class.java)
                
                val matchesSnapshot = db.collection("tournaments").document(tournamentId).collection("matches").get().await()
                matches = matchesSnapshot.toObjects(TournamentMatch::class.java)
                
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    LaunchedEffect(tournamentId) {
        loadTournamentData()
    }

    Scaffold(
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = { Text(tournament?.name?.uppercase() ?: "TOURNAMENT", color = goldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        bottomBar = {
            if (tournament != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(cardBg).padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showStreamOptions = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = tealAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tealAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Livestream", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = goldAccent)
            }
        } else if (error != null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Video Player Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    if (tournament?.thumbnail != null) {
                        AsyncImage(
                            model = tournament!!.thumbnail,
                            contentDescription = "Tournament Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )
                    }
                    val isLive = tournament?.status.equals("Live", ignoreCase = true)
                    
                    if (isLive) {
                        Surface(color = Color.Red.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { onNavigateToStream(tournament!!.id, false) },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .background(goldAccent.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                    
                    Text(
                        if (isLive) "Click to join stream" else "Waiting for host to start stream...",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    )
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = DetailTab.values().indexOf(selectedTab),
                    containerColor = cardBg,
                    contentColor = goldAccent,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[DetailTab.values().indexOf(selectedTab)]),
                            color = goldAccent
                        )
                    }
                ) {
                    DetailTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (selectedTab == tab) goldAccent else Color.Gray) }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        DetailTab.SCORES -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (matches.isEmpty()) {
                                    item { Text("No matches scheduled yet.", color = Color.Gray) }
                                } else {
                                    items(matches) { match ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("MATCH", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    // Team 1
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1F2937)), contentAlignment = Alignment.Center) {
                                                            Text(match.team1.take(1).uppercase(), color = tealAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(match.team1.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(4.dp)) {
                                                            IconButton(onClick = {
                                                                coroutineScope.launch {
                                                                    if (match.team1Score > 0) {
                                                                        db.collection("tournaments").document(tournament!!.id).collection("matches").document(match.id).update("team1Score", match.team1Score - 1).await()
                                                                        loadTournamentData()
                                                                    }
                                                                }
                                                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, contentDescription = "-", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                                                            Text("${match.team1Score}", color = goldAccent, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                                            IconButton(onClick = {
                                                                coroutineScope.launch {
                                                                    db.collection("tournaments").document(tournament!!.id).collection("matches").document(match.id).update("team1Score", match.team1Score + 1).await()
                                                                    loadTournamentData()
                                                                }
                                                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, contentDescription = "+", tint = tealAccent, modifier = Modifier.size(16.dp)) }
                                                        }
                                                    }

                                                    Text("VS", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))

                                                    // Team 2
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1F2937)), contentAlignment = Alignment.Center) {
                                                            Text(match.team2.take(1).uppercase(), color = redAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(match.team2.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(4.dp)) {
                                                            IconButton(onClick = {
                                                                coroutineScope.launch {
                                                                    if (match.team2Score > 0) {
                                                                        db.collection("tournaments").document(tournament!!.id).collection("matches").document(match.id).update("team2Score", match.team2Score - 1).await()
                                                                        loadTournamentData()
                                                                    }
                                                                }
                                                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, contentDescription = "-", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                                                            Text("${match.team2Score}", color = goldAccent, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                                            IconButton(onClick = {
                                                                coroutineScope.launch {
                                                                    db.collection("tournaments").document(tournament!!.id).collection("matches").document(match.id).update("team2Score", match.team2Score + 1).await()
                                                                    loadTournamentData()
                                                                }
                                                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, contentDescription = "+", tint = redAccent, modifier = Modifier.size(16.dp)) }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            db.collection("tournaments").document(tournament!!.id).collection("matches").document(match.id).delete().await()
                                                            loadTournamentData()
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "End Match", tint = redAccent, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("End Match", color = redAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        DetailTab.CHAT -> {
                            ChatComponent(tournamentId = tournament!!.id)
                        }
                        DetailTab.DETAILS -> {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text("EVENT DETAILS", color = goldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Status", color = Color.Gray, fontSize = 14.sp)
                                                Text(tournament!!.status, color = when(tournament!!.status) {
                                                    "Live" -> Color.Red
                                                    "Completed" -> tealAccent
                                                    "Cancelled" -> Color.Gray
                                                    else -> Color.White
                                                }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2D3748))
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Game", color = Color.Gray, fontSize = 14.sp)
                                                Text(tournament!!.game, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2D3748))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Prize Pool", color = Color.Gray, fontSize = 14.sp)
                                                Text(tournament!!.prize.ifEmpty { "None / Bragging Rights" }, color = tealAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2D3748))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Start Time", color = Color.Gray, fontSize = 14.sp)
                                                Text(tournament!!.time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2D3748))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            val totalPlayers = matches.sumOf { it.team1Players.size + it.team2Players.size }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Players", color = Color.Gray, fontSize = 14.sp)
                                                Text("$totalPlayers / Max: ${tournament!!.maxPlayers.ifEmpty { "Unlimited" }}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF8B5CF6)), contentAlignment = Alignment.Center) {
                                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text("Hosted by", color = Color.Gray, fontSize = 10.sp)
                                                    Text("Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
                                            IconButton(onClick = {}, modifier = Modifier.background(Color(0xFF1F2937), CircleShape)) {
                                                Icon(Icons.Default.Email, contentDescription = "Message Host", tint = tealAccent, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        DetailTab.TEAMS -> {
                            val totalPlayers = matches.sumOf { it.team1Players.size + it.team2Players.size }
                            val maxP = tournament!!.maxPlayers.toIntOrNull() ?: Int.MAX_VALUE
                            val canAddPlayer = totalPlayers < maxP

                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Registered Teams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Button(onClick = { showAddMatchDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = tealAccent)) {
                                            Text("Add Teams", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (matches.isEmpty()) {
                                    item { Text("No teams registered yet.", color = Color.Gray) }
                                } else {
                                    items(matches) { match ->
                                        // Team 1
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("TEAM ${match.team1.uppercase()}", color = tealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                            IconButton(onClick = { teamToEdit = Triple(match.id, true, match.team1) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Team Name", tint = tealAccent, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        match.team1Players.forEach { player ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF1F2937)), contentAlignment = Alignment.Center) {
                                                        Text(player.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(player.role.ifEmpty { "Player" }, color = Color.Gray, fontSize = 10.sp)
                                                    }
                                                    IconButton(onClick = { playerToEdit = Triple(match.id, true, player) }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Player", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (canAddPlayer) {
                                            TextButton(onClick = { playerToAdd = Pair(match.id, true) }) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = tealAccent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Player to ${match.team1}", color = tealAccent, fontSize = 12.sp)
                                            }
                                        } else {
                                            Text("Tournament is full", color = redAccent, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Team 2
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("TEAM ${match.team2.uppercase()}", color = redAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                            IconButton(onClick = { teamToEdit = Triple(match.id, false, match.team2) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Team Name", tint = redAccent, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        match.team2Players.forEach { player ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF1F2937)), contentAlignment = Alignment.Center) {
                                                        Text(player.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(player.role.ifEmpty { "Player" }, color = Color.Gray, fontSize = 10.sp)
                                                    }
                                                    IconButton(onClick = { playerToEdit = Triple(match.id, false, player) }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Player", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }

                                        if (canAddPlayer) {
                                            TextButton(onClick = { playerToAdd = Pair(match.id, false) }) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = redAccent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Player to ${match.team2}", color = redAccent, fontSize = 12.sp)
                                            }
                                        } else {
                                            Text("Tournament is full", color = redAccent, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                        HorizontalDivider(color = Color(0xFF2D3748))
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showStreamOptions) {
            AlertDialog(
                onDismissRequest = { showStreamOptions = false },
                containerColor = cardBg,
                title = { Text("Start Livestream", color = Color.White) },
                text = { Text("Choose your streaming source.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = { onNavigateToStream(tournament!!.id, true); showStreamOptions = false },
                        colors = ButtonDefaults.buttonColors(containerColor = tealAccent)
                    ) { Text("Camera", color = Color.Black) }
                },
                dismissButton = {
                    Button(
                        onClick = { onNavigateToStream("${tournament!!.id}_screen", true); showStreamOptions = false },
                        colors = ButtonDefaults.buttonColors(containerColor = tealAccent)
                    ) { Text("Screen", color = Color.Black) }
                }
            )
        }

        if (showEditDialog && tournament != null) {
            var eName by remember { mutableStateOf(tournament!!.name) }
            var eGame by remember { mutableStateOf(tournament!!.game) }
            var ePrize by remember { mutableStateOf(tournament!!.prize) }
            var eTime by remember { mutableStateOf(tournament!!.time) }
            var eMaxPlayers by remember { mutableStateOf(tournament!!.maxPlayers) }
            var eStatus by remember { mutableStateOf(tournament!!.status) }
            
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = cardBg,
                title = { Text("Edit Tournament", color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = eGame, onValueChange = { eGame = it }, label = { Text("Game", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = ePrize, onValueChange = { ePrize = it }, label = { Text("Prize", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = eTime, onValueChange = { eTime = it }, label = { Text("Time", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = eMaxPlayers, onValueChange = { eMaxPlayers = it }, label = { Text("Max Players", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Status", color = Color.LightGray, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Open", "Upcoming", "Live", "Completed", "Cancelled").chunked(3).forEach { chunk ->
                                // For simplicity, we just use TextButtons wrapped if needed. Let's just use a flow row or Column of Rows
                            }
                        }
                        // Manual layout for status buttons to avoid overflow
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Open", "Upcoming", "Live").forEach { statusOption ->
                                TextButton(onClick = { eStatus = statusOption }, colors = ButtonDefaults.textButtonColors(contentColor = if (eStatus == statusOption) goldAccent else Color.Gray)) {
                                    Text(statusOption, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Completed", "Cancelled").forEach { statusOption ->
                                TextButton(onClick = { eStatus = statusOption }, colors = ButtonDefaults.textButtonColors(contentColor = if (eStatus == statusOption) goldAccent else Color.Gray)) {
                                    Text(statusOption, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val m = db.collection("tournaments").document(tournament!!.id).collection("matches").get().await()
                                    m.forEach { db.collection("tournaments").document(tournament!!.id).collection("matches").document(it.id).delete().await() }
                                    
                                    val c = db.collection("tournaments").document(tournament!!.id).collection("chatMessages").get().await()
                                    c.forEach { db.collection("tournaments").document(tournament!!.id).collection("chatMessages").document(it.id).delete().await() }
                                    
                                    db.collection("tournaments").document(tournament!!.id).delete().await()
                                    showEditDialog = false
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = redAccent.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = redAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Tournament", color = redAccent)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            db.collection("tournaments").document(tournament!!.id).update(
                                mapOf(
                                    "name" to eName,
                                    "game" to eGame,
                                    "prize" to ePrize,
                                    "time" to eTime,
                                    "maxPlayers" to eMaxPlayers,
                                    "status" to eStatus
                                )
                            ).await()
                            showEditDialog = false
                            loadTournamentData()
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = goldAccent)) {
                        Text("Save", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (showAddMatchDialog) {
            var t1Name by remember { mutableStateOf("") }
            var t2Name by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showAddMatchDialog = false },
                containerColor = cardBg,
                title = { Text("Add Teams", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = t1Name, onValueChange = { t1Name = it }, label = { Text("Team 1 Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = t2Name, onValueChange = { t2Name = it }, label = { Text("Team 2 Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val ref = db.collection("tournaments").document(tournament!!.id).collection("matches").document()
                            ref.set(TournamentMatch(id = ref.id, team1 = t1Name, team2 = t2Name, team1Score = 0, team2Score = 0)).await()
                            showAddMatchDialog = false
                            loadTournamentData()
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = goldAccent)) {
                        Text("Add", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMatchDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (teamToEdit != null) {
            var newName by remember { mutableStateOf(teamToEdit!!.third) }
            AlertDialog(
                onDismissRequest = { teamToEdit = null },
                containerColor = cardBg,
                title = { Text("Edit Team Name", color = Color.White) },
                text = {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val field = if (teamToEdit!!.second) "team1" else "team2"
                            db.collection("tournaments").document(tournament!!.id).collection("matches").document(teamToEdit!!.first).update(field, newName).await()
                            teamToEdit = null
                            loadTournamentData()
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = goldAccent)) {
                        Text("Save", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { teamToEdit = null }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (playerToAdd != null) {
            var pName by remember { mutableStateOf("") }
            var pRole by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { playerToAdd = null },
                containerColor = cardBg,
                title = { Text("Add Player", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Player Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = pRole, onValueChange = { pRole = it }, label = { Text("Role (e.g. Captain, Sniper)", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val m = matches.find { it.id == playerToAdd!!.first }
                            if (m != null) {
                                val newPlayer = TournamentPlayer(id = java.util.UUID.randomUUID().toString(), name = pName, role = pRole)
                                val field = if (playerToAdd!!.second) "team1Players" else "team2Players"
                                val currentList = if (playerToAdd!!.second) m.team1Players else m.team2Players
                                db.collection("tournaments").document(tournament!!.id).collection("matches").document(m.id).update(field, currentList + newPlayer).await()
                                playerToAdd = null
                                loadTournamentData()
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = goldAccent)) {
                        Text("Add", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { playerToAdd = null }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (playerToEdit != null) {
            var pName by remember { mutableStateOf(playerToEdit!!.third.name) }
            var pRole by remember { mutableStateOf(playerToEdit!!.third.role) }
            AlertDialog(
                onDismissRequest = { playerToEdit = null },
                containerColor = cardBg,
                title = { Text("Edit Player", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Player Name", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        OutlinedTextField(value = pRole, onValueChange = { pRole = it }, label = { Text("Role", color = Color.LightGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val m = matches.find { it.id == playerToEdit!!.first }
                            if (m != null) {
                                val field = if (playerToEdit!!.second) "team1Players" else "team2Players"
                                val currentList = if (playerToEdit!!.second) m.team1Players else m.team2Players
                                val updatedList = currentList.map { if (it.id == playerToEdit!!.third.id) it.copy(name = pName, role = pRole) else it }
                                db.collection("tournaments").document(tournament!!.id).collection("matches").document(m.id).update(field, updatedList).await()
                                playerToEdit = null
                                loadTournamentData()
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = goldAccent)) {
                        Text("Save", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val m = matches.find { it.id == playerToEdit!!.first }
                            if (m != null) {
                                val field = if (playerToEdit!!.second) "team1Players" else "team2Players"
                                val currentList = if (playerToEdit!!.second) m.team1Players else m.team2Players
                                val updatedList = currentList.filter { it.id != playerToEdit!!.third.id }
                                db.collection("tournaments").document(tournament!!.id).collection("matches").document(m.id).update(field, updatedList).await()
                                playerToEdit = null
                                loadTournamentData()
                            }
                        }
                    }, colors = ButtonDefaults.textButtonColors(contentColor = redAccent)) { Text("Delete Player", color = redAccent) }
                }
            )
        }
    }
}
