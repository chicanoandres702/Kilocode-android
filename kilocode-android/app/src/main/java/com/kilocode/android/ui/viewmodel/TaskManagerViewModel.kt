package com.kilocode.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kilocode.android.data.repository.TaskManagerRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TaskManagerViewModel(private val repository: TaskManagerRepository) : ViewModel() {
    val tasks = repository.tasks

    fun refreshTasks() {
        viewModelScope.launch {
            repository.fetchBackgroundTasks()
        }
    }

    fun cancelTask(taskId: String) {
        repository.cancelTask(taskId)
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            repository.retryTask(taskId)
            repository.fetchBackgroundTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            repository.fetchBackgroundTasks()
        }
    }
}

class TaskManagerViewModelFactory(private val repository: TaskManagerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskManagerViewModel(repository) as T
    }
}
