package com.example.tododemo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PointsPolicyTest {

    @Test
    fun pointsPerTodoIsTen() {
        assertThat(PointsPolicy.POINTS_PER_TODO).isEqualTo(10)
    }
}
