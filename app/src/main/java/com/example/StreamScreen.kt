package com.example

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.ScreenCaptureParameters
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(channelName: String, isBroadcaster: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val isScreenShare = channelName.endsWith("_screen")
    
    var permissionsGranted by remember { mutableStateOf(!isBroadcaster) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    LaunchedEffect(isBroadcaster) {
        if (isBroadcaster) {
            val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            val hasPermissions = requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (hasPermissions) {
                permissionsGranted = true
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }

    DisposableEffect(channelName, permissionsGranted) {
        if (!permissionsGranted) {
            return@DisposableEffect onDispose {}
        }
        val agoraAppId = "7f5b5c318ba440bcbbe0cb7246388b4c"
        if (agoraAppId.isEmpty()) {
            error = "Agora App ID is missing."
        } else {
            val uid = (1..100000).random()
            val actualChannelName = channelName.removeSuffix("_screen")
            val roleStr = if (isBroadcaster) "publisher" else "subscriber"
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://agora-token-server-phi-pearl.vercel.app/api/token")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val jsonBody = """
                        {
                            "channel": "$actualChannelName",
                            "uid": $uid,
                            "role": "$roleStr"
                        }
                    """.trimIndent()
                    
                    conn.outputStream.use { os ->
                        val input = jsonBody.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                    
                    var fetchedToken: String? = null
                    if (conn.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val tokenRegex = """"token"\s*:\s*"([^"]+)"""".toRegex()
                        fetchedToken = tokenRegex.find(response)?.groupValues?.get(1)
                    }

                    withContext(Dispatchers.Main) {
                        if (fetchedToken == null) {
                            error = "Failed to fetch Agora token."
                            return@withContext
                        }

                        try {
                            val config = RtcEngineConfig()
                            config.mContext = context
                            config.mAppId = agoraAppId
                            config.mEventHandler = object : IRtcEngineEventHandler() {
                                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                                    super.onJoinChannelSuccess(channel, uid, elapsed)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        FileLogger.logInfo("Agora", "Joined channel: $channel with uid: $uid", context)
                                    }
                                }
                                override fun onUserJoined(uid: Int, elapsed: Int) {
                                    super.onUserJoined(uid, elapsed)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        FileLogger.logInfo("Agora", "User joined: $uid", context)
                                        if (!isBroadcaster) {
                                            remoteUid = uid
                                        }
                                    }
                                }
                                override fun onUserOffline(uid: Int, reason: Int) {
                                    super.onUserOffline(uid, reason)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        FileLogger.logInfo("Agora", "User offline: $uid, reason: $reason", context)
                                        if (!isBroadcaster && remoteUid == uid) {
                                            remoteUid = null
                                        }
                                    }
                                }
                                override fun onError(err: Int) {
                                    super.onError(err)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        error = "Agora Error code: $err"
                                        FileLogger.logError("Agora", "Agora Error code: $err", null, context)
                                    }
                                }
                            }
                            
                            val engine = RtcEngine.create(config)
                            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                            engine.setClientRole(if (isBroadcaster) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE)
                            engine.enableVideo()
                            
                            if (isBroadcaster) {
                                if (isScreenShare) {
                                    val parameters = ScreenCaptureParameters()
                                    parameters.captureVideo = true
                                    parameters.captureAudio = true
                                    engine.startScreenCapture(parameters)
                                } else {
                                    val surfaceView = SurfaceView(context)
                                    surfaceView.setZOrderMediaOverlay(true)
                                    engine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                                    engine.startPreview()
                                    localSurfaceView = surfaceView
                                }
                                FirebaseFirestore.getInstance().collection("tournaments").document(actualChannelName).update("status", "Live")
                            }
                            
                            val options = io.agora.rtc2.ChannelMediaOptions()
                            options.clientRoleType = if (isBroadcaster) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
                            options.publishCameraTrack = isBroadcaster && !isScreenShare
                            options.publishMicrophoneTrack = isBroadcaster
                            options.publishScreenCaptureVideo = isBroadcaster && isScreenShare
                            options.publishScreenCaptureAudio = isBroadcaster && isScreenShare
                            options.autoSubscribeAudio = true
                            options.autoSubscribeVideo = true
                            
                            engine.joinChannel(fetchedToken, actualChannelName, uid, options)
                            
                            rtcEngine = engine
                        } catch (e: Exception) {
                            error = "Failed to init Agora: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        error = "Token server error: ${e.message}"
                    }
                }
            }
        }
        
        onDispose {
            if (isBroadcaster) {
                val actualChannelName = channelName.removeSuffix("_screen")
                FirebaseFirestore.getInstance().collection("tournaments").document(actualChannelName).update("status", "Offline")
            }
            if (isBroadcaster && isScreenShare) {
                rtcEngine?.stopScreenCapture()
            }
            rtcEngine?.leaveChannel()
            rtcEngine?.stopPreview()
            RtcEngine.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isScreenShare) "Live Screen Share" else "Live Stream") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isBroadcaster && !isScreenShare && localSurfaceView != null) {
                AndroidView(
                    factory = { localSurfaceView!! },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isBroadcaster && isScreenShare) {
                Text("Screen sharing in progress...", modifier = Modifier.align(Alignment.Center))
            } else if (!isBroadcaster && remoteUid != null) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            setZOrderMediaOverlay(true)
                            rtcEngine?.setupRemoteVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!))
                        }
                    },
                    update = { view ->
                        rtcEngine?.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!permissionsGranted) {
                Text("Waiting for permissions...", modifier = Modifier.align(Alignment.Center))
            } else {
                Text("Waiting for stream...", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
