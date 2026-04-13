package pl.wsei.pam.lab06.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.wsei.pam.lab06.Priority
import pl.wsei.pam.lab06.TodoTask
import java.time.LocalDate

@Entity(tableName = "tasks")
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val deadline: LocalDate,
    var isDone: Boolean,
    val priority: Priority
) {
    fun toModel(): TodoTask {
        return TodoTask(
            id = id,
            deadline = deadline,
            isDone = isDone,
            priority = priority,
            title = title
        )
    }

    companion object {
        fun fromModel(model: TodoTask): TodoTaskEntity {
            return TodoTaskEntity(
                id = model.id,
                title = model.title,
                priority = model.priority,
                isDone = model.isDone,
                deadline = model.deadline
            )
        }
    }
}
