package pl.wsei.pam.lab06.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.wsei.pam.lab06.TodoApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ListViewModel(
                repository = todoApplication().container.todoTaskRepository
            )
        }
        initializer {
            FormViewModel(
                repository = todoApplication().container.todoTaskRepository,
                dateProvider = todoApplication().container.dateProvider
            )
        }
    }
}

fun CreationExtras.todoApplication(): TodoApplication {
    val app = this[APPLICATION_KEY]
    return app as TodoApplication
}
