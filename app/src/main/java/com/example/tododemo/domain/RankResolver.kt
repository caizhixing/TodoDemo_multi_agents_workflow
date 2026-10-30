package com.example.tododemo.domain

import com.example.tododemo.domain.model.RankTier

object RankResolver {

    private val tiers: List<Pair<Int, RankTier>> = listOf(
        0 to RankTier.NOVICE,
        30 to RankTier.APPRENTICE,
        80 to RankTier.DILIGENT,
        150 to RankTier.EXPERT,
        300 to RankTier.LEGEND,
    )

    fun resolveForPoints(points: Int): RankTier =
        tiers.lastOrNull { points >= it.first }?.second ?: RankTier.NOVICE

    fun displayName(tier: RankTier): String = when (tier) {
        RankTier.NOVICE -> "新手"
        RankTier.APPRENTICE -> "见习"
        RankTier.DILIGENT -> "勤奋"
        RankTier.EXPERT -> "达人"
        RankTier.LEGEND -> "传奇"
    }
}
