package com.example

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
data class TournamentDetailRoute(val tournamentId: String)

@Serializable
data class StreamRoute(val channelName: String, val isBroadcaster: Boolean)

@Serializable
data class ChatRoute(val tournamentId: String)
