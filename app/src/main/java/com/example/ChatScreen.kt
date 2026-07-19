package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "User"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(tournamentId: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournament Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF0F1115))) {
            ChatComponent(tournamentId = tournamentId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatComponent(tournamentId: String, modifier: Modifier = Modifier) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val darkBg = Color(0xFF0F1115)
    val cardBg = Color(0xFF16181E)
    val goldAccent = Color(0xFFD4AF37)

    LaunchedEffect(tournamentId) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("tournaments").document(tournamentId).collection("chatMessages")
                .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    error = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages = snapshot.toObjects(ChatMessage::class.java)
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to connect to chat (Check google-services.json)"
        }
    }

    Column(modifier = modifier.fillMaxSize().background(darkBg)) {
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            items(messages) { msg ->
                val isMe = msg.senderId == FirebaseAuth.getInstance().currentUser?.uid
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isMe) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF2563EB)), contentAlignment = Alignment.Center) {
                            Text(msg.senderName.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                        Text("${if (isMe) "You" else msg.senderName}", color = if (isMe) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) cardBg else cardBg.copy(alpha = 0.5f)
                            ),
                            shape = if (isMe) RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp) else RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp),
                            border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D3748))
                        ) {
                            Text(msg.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    
                    if (isMe) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF10B981)), contentAlignment = Alignment.Center) {
                            Text("ME", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Send a message...", color = Color.Gray, fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = goldAccent,
                    unfocusedBorderColor = Color(0xFF2D3748)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val ref = db.collection("tournaments").document(tournamentId).collection("chatMessages").document()
                                val auth = FirebaseAuth.getInstance()
                                val msg = ChatMessage(
                                    id = ref.id,
                                    text = text,
                                    senderId = auth.currentUser?.uid ?: "unknown",
                                    senderName = auth.currentUser?.email?.substringBefore("@") ?: "User"
                                )
                                ref.set(msg).await()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }
                },
                modifier = Modifier.size(48.dp).background(goldAccent, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}
