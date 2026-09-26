package com.freedu.socialbaby.data.repository

import com.freedu.socialbaby.data.local.dao.ShelfDao
import com.freedu.socialbaby.data.mapper.toDomain
import com.freedu.socialbaby.data.mapper.toEntity
import com.freedu.socialbaby.domain.model.Shelf
import com.freedu.socialbaby.domain.repository.ShelfRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShelfRepositoryImpl @Inject constructor(
    private val dao: ShelfDao
) : ShelfRepository {
    override fun observeAll(): Flow<List<Shelf>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<Shelf> = dao.getAll().map { it.toDomain() }
    override suspend fun getById(id: Long): Shelf? = dao.getById(id)?.toDomain()
    override suspend fun insert(shelf: Shelf): Long = dao.insert(shelf.toEntity())
    override suspend fun update(shelf: Shelf) = dao.update(shelf.toEntity())
    override suspend fun delete(shelf: Shelf) = dao.delete(shelf.toEntity())
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
