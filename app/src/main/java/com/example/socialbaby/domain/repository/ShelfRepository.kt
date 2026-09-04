package com.example.socialbaby.domain.repository

import com.example.socialbaby.domain.model.Shelf
import kotlinx.coroutines.flow.Flow

interface ShelfRepository {
    fun observeAll(): Flow<List<Shelf>>
    suspend fun getAll(): List<Shelf>
    suspend fun getById(id: Long): Shelf?
    suspend fun insert(shelf: Shelf): Long
    suspend fun update(shelf: Shelf)
    suspend fun delete(shelf: Shelf)
    suspend fun deleteById(id: Long)
}
