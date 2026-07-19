package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val darkBackground = Color(0xFF0F0C29)
val accentNeon = Color(0xFF00FF87)
val cardBg = Color(0xFF1A1A2E)

data class MockUser(val id: String, val name: String, val avatarColor: Color)

data class MockTournament(
    val id: String,
    val title: String,
    val game: String,
    val prizePool: String?,
    val maxPlayers: Int,
    val registeredPlayers: List<MockUser>,
    val host: MockUser,
    val isLive: Boolean,
    val isFinished: Boolean = false,
    val myScore: String? = null, // e.g. "Won", "Lost", "3rd Place"
    val gradient: List<Color>
)

val mockUsers = listOf(
    MockUser("1", "ProGamer99", Color(0xFFFF416C)),
    MockUser("2", "NinjaSniper", Color(0xFF11998e)),
    MockUser("3", "HostMaster", Color(0xFF8A2387))
)

val mockData = listOf(
    MockTournament("t1", "PUBG Global Series 2026", "PUBG Mobile", "$2.5M", 128, mockUsers.take(2), mockUsers[2], isLive = true, gradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
    MockTournament("t2", "Valorant Champions Tour", "Valorant", null, 64, mockUsers, mockUsers[0], isLive = false, gradient = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))),
    MockTournament("t3", "Call of Duty: Mobile Masters", "CODM", "$500K", 256, emptyList(), mockUsers[1], isLive = false, isFinished = true, myScore = "Won 1st Place", gradient = listOf(Color(0xFF00B4DB), Color(0xFF0083B0))),
    MockTournament("t4", "Free Fire World Series", "Free Fire", "$2.0M", 100, mockUsers, mockUsers[2], isLive = false, gradient = listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
)

sealed class MockupRoute {
    object Home : MockupRoute()
    data class LiveStream(val tournament: MockTournament) : MockupRoute()
    data class DM(val user: MockUser) : MockupRoute()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignMockupScreen(onBack: () -> Unit) {
    var currentRoute by remember { mutableStateOf<MockupRoute>(MockupRoute.Home) }

    Scaffold(
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val route = currentRoute) {
                            is MockupRoute.Home -> "eSports Tournaments"
                            is MockupRoute.LiveStream -> route.tournament.title
                            is MockupRoute.DM -> "Chat: ${route.user.name}"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (currentRoute is MockupRoute.Home) onBack()
                        else currentRoute = MockupRoute.Home
                    }) {
                        Text("< Back", color = accentNeon)
                    }
                },
                actions = {
                    if (currentRoute is MockupRoute.Home) {
                        IconButton(onClick = { /* Open messages list */ }) {
                            Icon(Icons.Default.Email, contentDescription = "Messages", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (currentRoute is MockupRoute.Home) {
                NavigationBar(
                    containerColor = darkBackground,
                    contentColor = accentNeon,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = true,
                        onClick = { },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentNeon, unselectedIconColor = Color.Gray,
                            selectedTextColor = accentNeon, unselectedTextColor = Color.Gray, indicatorColor = cardBg
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        selected = false,
                        onClick = { },
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
                        label = { Text("Notifications") },
                        selected = false,
                        onClick = { },
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = false,
                        onClick = { },
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val route = currentRoute) {
                is MockupRoute.Home -> MockHomeContent { newRoute -> currentRoute = newRoute }
                is MockupRoute.LiveStream -> MockLiveStreamContent(route.tournament)
                is MockupRoute.DM -> MockDMContent(route.user)
            }
        }
    }
}

@Composable
fun MockHomeContent(onNavigate: (MockupRoute) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Trending Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        items(mockData) { tournament ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (tournament.isLive) onNavigate(MockupRoute.LiveStream(tournament))
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(tournament.gradient)))
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                        
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            if (tournament.isLive) {
                                Surface(color = Color.Red.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else if (tournament.isFinished) {
                                Surface(color = Color.DarkGray.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text("FINISHED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(tournament.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    }
                    
                    // Tournament Details Section
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${tournament.game} | ${tournament.registeredPlayers.size}/${tournament.maxPlayers} Players", color = Color.LightGray, fontSize = 12.sp)
                            if (tournament.prizePool != null) {
                                Text("Prize: ${tournament.prizePool}", color = accentNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        
                        if (tournament.myScore != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Result: ${tournament.myScore}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Host:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigate(MockupRoute.DM(tournament.host)) }
                            ) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(tournament.host.avatarColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tournament.host.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Send, contentDescription = "DM", tint = accentNeon, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun MockLiveStreamContent(tournament: MockTournament) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Video Player Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Brush.linearGradient(tournament.gradient))
        ) {
            Text("Live Video Playing...", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            
            // Video Controls Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quality Toggle
                Surface(color = Color(0x88000000), shape = RoundedCornerShape(4.dp)) {
                    Text("1080p", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                // Fullscreen Toggle
                Icon(Icons.Default.Settings, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        // Live Chat Section
        Column(modifier = Modifier.fillMaxSize().background(cardBg)) {
            Text(
                "Live Chat (Agora RTM Demo)",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(10) { index ->
                    val isMe = index % 3 == 0
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(if (isMe) "You: " else "User$index: ", color = if (isMe) accentNeon else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (isMe) "Let's gooo!" else "Nice play!", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            
            // Chat Input
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Say something...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = darkBackground,
                        unfocusedContainerColor = darkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {}, modifier = Modifier.background(accentNeon, CircleShape)) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = darkBackground)
                }
            }
        }
    }
}

@Composable
fun MockDMContent(user: MockUser) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
            item {
                Text("This is the beginning of your chat with ${user.name}", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
                    Surface(color = cardBg, shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)) {
                        Text("Hey, are you joining the tournament tomorrow?", color = Color.White, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
                    Surface(color = accentNeon.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)) {
                        Text("Yes! See you in the lobby.", color = Color.White, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Message...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {}, modifier = Modifier.background(accentNeon, CircleShape)) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = darkBackground)
            }
        }
    }
}

