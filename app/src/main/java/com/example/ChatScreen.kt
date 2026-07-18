package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(messages) { msg ->
                    val isMe = msg.senderId == FirebaseAuth.getInstance().currentUser?.uid
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(msg.senderName, style = MaterialTheme.typography.labelSmall)
                                Text(msg.text)
                            }
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
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
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
