package pl.wsei.pam.lab06.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoTaskDao {
    @Insert
    suspend fun insertAll(vararg tasks: TodoTaskEntity)

    @Delete
    suspend fun removeById(item: TodoTaskEntity)

    @Update
    suspend fun update(item: TodoTaskEntity)

    @Query("SELECT * FROM tasks ORDER BY deadline DESC")
    fun findAll(): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun find(id: Int): Flow<TodoTaskEntity>
}
