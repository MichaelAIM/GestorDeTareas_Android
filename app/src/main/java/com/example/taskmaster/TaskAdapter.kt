package com.example.taskmaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView // ¡NUEVO! Importar ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(private val tasks: MutableList<Task>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Interfaz para manejar clics en los items
    interface OnItemClickListener {
        fun onItemClick(task: Task)
        fun onCheckboxClick(task: Task, isChecked: Boolean)
        fun onDeleteClick(task: Task) // ¡NUEVO! Para el botón de eliminar
    }
    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.taskName.text = currentTask.name
        holder.taskCategory.text = currentTask.category
        holder.taskCompleted.isChecked = currentTask.isCompleted

        holder.itemView.setOnClickListener {
            listener?.onItemClick(currentTask)
        }
        holder.taskCompleted.setOnCheckedChangeListener { _, isChecked ->
            // Evitar que el listener se dispare recursivamente si el estado se cambia programáticamente
            // Se maneja con setOnClickListener para evitar bucles si se actualiza programáticamente
        }
        holder.taskCompleted.setOnClickListener {
            // Actualizamos el estado en la lista y notificamos
            currentTask.isCompleted = holder.taskCompleted.isChecked
            listener?.onCheckboxClick(currentTask, currentTask.isCompleted)
            // No es necesario notificar cambios aquí si el listener lo maneja
            // o si la actualización de datos viene de una fuente externa (ViewModel, DB)
        }

        // ¡NUEVO! Listener para el botón de eliminar
        holder.imageViewDeleteTask.setOnClickListener {
            listener?.onDeleteClick(currentTask)
        }
    }

    override fun getItemCount() = tasks.size

    fun addTask(task: Task) {
        tasks.add(task)
        notifyItemInserted(tasks.size - 1)
    }

    fun updateTask(updatedTask: Task) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
            notifyItemChanged(index)
        }
    }

    fun removeTask(task: Task) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun setTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged() // O usar DiffUtil para mejor rendimiento
    }


    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskName: TextView = itemView.findViewById(R.id.textViewTaskNameItem)
        val taskCategory: TextView = itemView.findViewById(R.id.textViewTaskCategoryItem)
        val taskCompleted: CheckBox = itemView.findViewById(R.id.checkBoxTaskCompletedItem)
        val imageViewDeleteTask: ImageView = itemView.findViewById(R.id.imageViewDeleteTask) // ¡NUEVO!
    }
}