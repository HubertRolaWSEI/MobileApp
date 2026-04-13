package pl.wsei.pam.lab06.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import pl.wsei.pam.lab06.Priority
import pl.wsei.pam.lab06.TodoTask
import pl.wsei.pam.lab06.data.CurrentDateProvider
import pl.wsei.pam.lab06.data.LocalDateConverter
import pl.wsei.pam.lab06.data.TodoTaskRepository
import java.time.LocalDate

class FormViewModel(
    private val repository: TodoTaskRepository,
    private val dateProvider: CurrentDateProvider
) : ViewModel() {

    var todoTaskUiState by mutableStateOf(TodoTaskUiState())
        private set

    var isEditMode by mutableStateOf(false)
        private set

    suspend fun loadTask(id: Int) {
        repository.getItemAsStream(id).collect { task ->
            if (task != null) {
                isEditMode = true
                todoTaskUiState = task.toTodoTaskUiState(isValid = true)
            }
        }
    }

    suspend fun save() {
        if (validate()) {
            val task = todoTaskUiState.todoTask.toTodoTask()
            if (isEditMode) {
                repository.updateItem(task)
            } else {
                repository.insertItem(task)
            }
        }
    }

    fun updateUiState(todoTaskForm: TodoTaskForm) {
        todoTaskUiState = TodoTaskUiState(
            todoTask = todoTaskForm,
            isValid = validate(todoTaskForm)
        )
    }

    private fun validate(uiState: TodoTaskForm = todoTaskUiState.todoTask): Boolean {
        return with(uiState) {
            title.isNotBlank() &&
                    !LocalDateConverter.fromMillis(deadline).isBefore(dateProvider.currentDate)
        }
    }
}

data class TodoTaskUiState(
    var todoTask: TodoTaskForm = TodoTaskForm(),
    val isValid: Boolean = false
)

data class TodoTaskForm(
    val id: Int = 0,
    val title: String = "",
    val deadline: Long = LocalDateConverter.toMillis(LocalDate.now()),
    val isDone: Boolean = false,
    val priority: String = Priority.Low.name
)

fun TodoTask.toTodoTaskUiState(isValid: Boolean = false): TodoTaskUiState = TodoTaskUiState(
    todoTask = this.toTodoTaskForm(),
    isValid = isValid
)

fun TodoTaskForm.toTodoTask(): TodoTask = TodoTask(
    id = id,
    title = title,
    deadline = LocalDateConverter.fromMillis(deadline),
    isDone = isDone,
    priority = Priority.valueOf(priority)
)

fun TodoTask.toTodoTaskForm(): TodoTaskForm = TodoTaskForm(
    id = id,
    title = title,
    deadline = LocalDateConverter.toMillis(deadline),
    isDone = isDone,
    priority = priority.name
)