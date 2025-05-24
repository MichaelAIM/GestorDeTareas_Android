package com.example.taskmaster

data class Task(
    val id: String, // Identificador único para la tarea
    val name: String,
    val category: String,
    val priority: String, // ¡NUEVO! Prioridad de la tarea
    var isCompleted: Boolean = false
)