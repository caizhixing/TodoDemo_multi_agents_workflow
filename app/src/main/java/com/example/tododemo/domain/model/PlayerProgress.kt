package com.example.tododemo.domain.model

data class PlayerProgress(
    val totalPoints: Int,
    val completedCount: Int,
    val rank: RankTier,
)
