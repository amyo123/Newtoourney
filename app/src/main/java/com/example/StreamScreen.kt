package com.example

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(channelName: String, isBroadcaster: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val isScreenShare = channelName.endsWith("_screen")

    DisposableEffect(channelName) {
        val agoraAppId = "7f5b5c318ba440bcbbe0cb7246388b4c"
        if (agoraAppId.isEmpty()) {
            error = "Agora App ID is missing."
        } else {
            try {
                val config = RtcEngineConfig()
                config.mContext = context
                config.mAppId = agoraAppId
                config.mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        super.onUserJoined(uid, elapsed)
                        if (!isBroadcaster) {
                            coroutineScope.launch {
                                withContext(Dispatchers.Main) {
                                    val surfaceView = SurfaceView(context)
                                    surfaceView.setZOrderMediaOverlay(true)
                                    rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
                                    remoteSurfaceView = surfaceView
                                }
                            }
                        }
                    }
                    override fun onUserOffline(uid: Int, reason: Int) {
                        super.onUserOffline(uid, reason)
                        if (!isBroadcaster) {
                            coroutineScope.launch {
                                withContext(Dispatchers.Main) {
                                    remoteSurfaceView = null
                                }
                            }
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
                        engine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                        engine.startPreview()
                        localSurfaceView = surfaceView
                    }
                }
                
                engine.joinChannel(null, channelName, "Extra Optional Data", 0)
                
                rtcEngine = engine
            } catch (e: Exception) {
                error = "Failed to init Agora: ${e.message}"
            }
        }
        
        onDispose {
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
            } else if (!isBroadcaster && remoteSurfaceView != null) {
                AndroidView(
                    factory = { remoteSurfaceView!! },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Waiting for stream...", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
