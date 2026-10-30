package com.example.tododemo.domain

import com.example.tododemo.domain.model.RankTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RankResolverTest {

    @Test
    fun resolvesTierLowerBoundaries() {
        assertThat(RankResolver.resolveForPoints(0)).isEqualTo(RankTier.NOVICE)
        assertThat(RankResolver.resolveForPoints(30)).isEqualTo(RankTier.APPRENTICE)
        assertThat(RankResolver.resolveForPoints(80)).isEqualTo(RankTier.DILIGENT)
        assertThat(RankResolver.resolveForPoints(150)).isEqualTo(RankTier.EXPERT)
        assertThat(RankResolver.resolveForPoints(300)).isEqualTo(RankTier.LEGEND)
        assertThat(RankResolver.resolveForPoints(301)).isEqualTo(RankTier.LEGEND)
    }

    @Test
    fun resolvesTierUpperBoundaries() {
        assertThat(RankResolver.resolveForPoints(29)).isEqualTo(RankTier.NOVICE)
        assertThat(RankResolver.resolveForPoints(79)).isEqualTo(RankTier.APPRENTICE)
        assertThat(RankResolver.resolveForPoints(149)).isEqualTo(RankTier.DILIGENT)
        assertThat(RankResolver.resolveForPoints(299)).isEqualTo(RankTier.EXPERT)
    }

    @Test
    fun negativePointsFallBackToNovice() {
        assertThat(RankResolver.resolveForPoints(-1)).isEqualTo(RankTier.NOVICE)
    }

    @Test
    fun displayNameCoversAllTiers() {
        assertThat(RankResolver.displayName(RankTier.NOVICE)).isEqualTo("新手")
        assertThat(RankResolver.displayName(RankTier.APPRENTICE)).isEqualTo("见习")
        assertThat(RankResolver.displayName(RankTier.DILIGENT)).isEqualTo("勤奋")
        assertThat(RankResolver.displayName(RankTier.EXPERT)).isEqualTo("达人")
        assertThat(RankResolver.displayName(RankTier.LEGEND)).isEqualTo("传奇")
    }
}
