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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(channelName: String, isBroadcaster: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

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
                        if (!isBroadcaster && remoteSurfaceView == null) {
                            // In a real app we'd dispatch to main thread, but surface view creation should ideally be on main
                        }
                    }
                }
                
                val engine = RtcEngine.create(config)
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                engine.setClientRole(if (isBroadcaster) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE)
                engine.enableVideo()
                
                if (isBroadcaster) {
                    val surfaceView = SurfaceView(context)
                    engine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                    engine.startPreview()
                    localSurfaceView = surfaceView
                }
                
                engine.joinChannel(null, channelName, "Extra Optional Data", 0)
                
                rtcEngine = engine
            } catch (e: Exception) {
                error = "Failed to init Agora: ${e.message}"
            }
        }
        
        onDispose {
            rtcEngine?.leaveChannel()
            rtcEngine?.stopPreview()
            RtcEngine.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Stream") },
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
            } else if (localSurfaceView != null) {
                AndroidView(
                    factory = { localSurfaceView!! },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
