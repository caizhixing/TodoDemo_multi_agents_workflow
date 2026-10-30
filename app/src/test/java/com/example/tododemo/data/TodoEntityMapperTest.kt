package com.example.tododemo.data

import com.example.tododemo.domain.model.TodoItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TodoEntityMapperTest {

    @Test
    fun entityToDomain_mapsAllFields() {
        val entity = TodoEntity(
            id = 7L,
            title = "买牛奶",
            isCompleted = true,
            createdAtMillis = 100L,
            completedAtMillis = 200L,
        )

        val item = entity.toDomain()

        assertThat(item).isEqualTo(TodoItem(id = 7L, title = "买牛奶", isCompleted = true, completedAtMillis = 200L))
    }

    @Test
    fun domainToEntity_mapsAllFields() {
        val item = TodoItem(id = 7L, title = "买牛奶", isCompleted = true, completedAtMillis = 200L)

        val entity = item.toEntity(createdAtMillis = 100L)

        assertThat(entity).isEqualTo(
            TodoEntity(
                id = 7L,
                title = "买牛奶",
                isCompleted = true,
                createdAtMillis = 100L,
                completedAtMillis = 200L,
            ),
        )
    }

    @Test
    fun roundTrip_entityToDomainAndBack_preservesAllFields() {
        val original = TodoEntity(
            id = 9L,
            title = "写周报",
            isCompleted = false,
            createdAtMillis = 300L,
            completedAtMillis = null,
        )

        val roundTripped = original.toDomain().toEntity(createdAtMillis = original.createdAtMillis)

        assertThat(roundTripped).isEqualTo(original)
    }
}
