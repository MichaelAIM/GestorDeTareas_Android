package com.example.taskmaster

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importar AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar // Importar Snackbar
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var editTextTaskName: EditText
    private lateinit var buttonAddTask: Button
    private lateinit var checkBoxTaskCompletedWidget: CheckBox // Renombrado para evitar colisión
    private lateinit var radioGroupPriority: RadioGroup
    private lateinit var progressBarProject: ProgressBar
    private lateinit var ratingBarApp: RatingBar
    private lateinit var spinnerTaskCategory: Spinner
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    // Vistas para mostrar detalles de la tarea seleccionada (ahora en TableLayout)
    private lateinit var textViewSelectedTaskName: TextView
    private lateinit var textViewSelectedTaskPriority: TextView
    private lateinit var textViewSelectedTaskStatus: TextView

    // ¡NUEVO! Vista para mostrar el promedio de calificaciones en el CardView
    private lateinit var textViewAverageRating: TextView
    // ¡NUEVO! Lista para almacenar las calificaciones
    private val ratingsList = ArrayList<Float>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.title_activity_main)


        // Inicializar Vistas
        editTextTaskName = findViewById(R.id.editTextTaskName)
        buttonAddTask = findViewById(R.id.buttonAddTask)
        checkBoxTaskCompletedWidget = findViewById(R.id.checkBoxTaskCompleted)
        radioGroupPriority = findViewById(R.id.radioGroupPriority)
        progressBarProject = findViewById(R.id.progressBarProject)
        ratingBarApp = findViewById(R.id.ratingBarApp)
        spinnerTaskCategory = findViewById(R.id.spinnerTaskCategory)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)

        // Inicializar los TextViews de detalles (ahora en TableLayout)
        textViewSelectedTaskName = findViewById(R.id.textViewSelectedTaskName)
        textViewSelectedTaskPriority = findViewById(R.id.textViewSelectedTaskPriority)
        textViewSelectedTaskStatus = findViewById(R.id.textViewSelectedTaskStatus)

        // ¡NUEVO! Inicializar el TextView para el promedio de calificaciones
        textViewAverageRating = findViewById(R.id.textViewAverageRating)


        // Configurar Spinner
        setupSpinner()

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar datos de ejemplo
        loadSampleTasks() // Esto ya llamará a updateProgressBar()

        // Configurar Listeners para Widgets
        buttonAddTask.setOnClickListener {
            addTaskFromInput()
        }

        checkBoxTaskCompletedWidget.setOnCheckedChangeListener { _, isChecked ->
            // Ahora usando Snackbar para mejor UX
            Snackbar.make(findViewById(android.R.id.content), "Checkbox general: ${if (isChecked) "Marcado" else "Desmarcado"}", Snackbar.LENGTH_SHORT).show()
        }

        radioGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            val selectedPriority = when (checkedId) {
                R.id.radioButtonHigh -> getString(R.string.high)
                R.id.radioButtonMedium -> getString(R.string.medium)
                R.id.radioButtonLow -> getString(R.string.low)
                else -> "N/A"
            }
            // Ahora usando Snackbar
            Snackbar.make(findViewById(android.R.id.content), "Prioridad seleccionada: $selectedPriority", Snackbar.LENGTH_SHORT).show()
        }

        // ¡MODIFICADO! Listener para RatingBar con diálogo y almacenamiento
        ratingBarApp.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                showSaveRatingDialog(rating)
            }
        }

        // Mostrar el promedio inicial (o N/A si no hay calificaciones)
        updateAverageRatingDisplay()

        // Eliminamos la simulación de ProgressBar aquí, ahora se maneja en updateProgressBar()
    }

    private fun setupSpinner() {
        val categories = arrayOf("Personal", "Trabajo", "Estudio", "Hogar", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTaskCategory.adapter = adapter

        spinnerTaskCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                Snackbar.make(findViewById(android.R.id.content), "Categoría seleccionada: $selectedCategory", Snackbar.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(taskList)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter

        taskAdapter.setOnItemClickListener(object : TaskAdapter.OnItemClickListener {
            override fun onItemClick(task: Task) {
                Snackbar.make(findViewById(android.R.id.content), "Tarea clicada: ${task.name}", Snackbar.LENGTH_SHORT).show()
                displayTaskDetails(task)
            }

            override fun onCheckboxClick(task: Task, isChecked: Boolean) {
                val status = if (isChecked) "completada" else "pendiente"
                Snackbar.make(findViewById(android.R.id.content), "${task.name} marcada como $status", Snackbar.LENGTH_SHORT).show()
                updateProgressBar()
                displayTaskDetails(task)
            }

            override fun onDeleteClick(task: Task) {
                taskAdapter.removeTask(task)
                Snackbar.make(findViewById(android.R.id.content), "Tarea '${task.name}' eliminada.", Snackbar.LENGTH_SHORT).show()
                updateProgressBar()
                if (textViewSelectedTaskName.text.contains(task.name)) {
                    clearTaskDetails()
                }
            }
        })
    }

    private fun loadSampleTasks() {
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_1_title), getString(R.string.sample_task_1_category), getString(R.string.medium), false))
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_2_title), getString(R.string.sample_task_2_category), getString(R.string.high), true))
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_3_title), getString(R.string.sample_task_3_category), getString(R.string.low), false))
        taskAdapter.notifyDataSetChanged()
        updateProgressBar()
    }

    private fun addTaskFromInput() {
        val taskName = editTextTaskName.text.toString().trim()
        val selectedCategory = spinnerTaskCategory.selectedItem.toString()

        val selectedPriorityId = radioGroupPriority.checkedRadioButtonId
        val priority: String = when (selectedPriorityId) {
            R.id.radioButtonHigh -> getString(R.string.high)
            R.id.radioButtonMedium -> getString(R.string.medium)
            R.id.radioButtonLow -> getString(R.string.low)
            else -> getString(R.string.medium)
        }

        if (taskName.isNotEmpty()) {
            val newTask = Task(UUID.randomUUID().toString(), taskName, selectedCategory, priority)
            taskAdapter.addTask(newTask)
            editTextTaskName.text.clear()
            radioGroupPriority.check(R.id.radioButtonMedium)
            Snackbar.make(findViewById(android.R.id.content), "Tarea agregada: $taskName (Prioridad: $priority)", Snackbar.LENGTH_SHORT).show()
            updateProgressBar()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Por favor, ingresa el nombre de la tarea.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun displayTaskDetails(task: Task) {
        textViewSelectedTaskName.text = "${task.name}"
        textViewSelectedTaskPriority.text = " ${task.priority}"
        val status = if (task.isCompleted) "Completada" else "Pendiente"
        textViewSelectedTaskStatus.text = "$status"
    }

    private fun clearTaskDetails() {
        textViewSelectedTaskName.text = "No seleccionado "
        textViewSelectedTaskPriority.text = "No seleccionado "
        textViewSelectedTaskStatus.text = "No seleccionado "
    }

    private fun updateProgressBar() {
        if (taskList.isEmpty()) {
            progressBarProject.progress = 0
            return
        }

        val completedTasksCount = taskList.count { it.isCompleted }
        val totalTasksCount = taskList.size
        val progress = (completedTasksCount.toDouble() / totalTasksCount * 100).toInt()
        progressBarProject.progress = progress
    }

    // ¡NUEVAS FUNCIONES PARA EL RATING BAR!
    private fun showSaveRatingDialog(rating: Float) {
        AlertDialog.Builder(this)
            .setTitle("Guardar Calificación")
            .setMessage("¿Deseas guardar tu calificación de $rating estrellas?")
            .setPositiveButton("Sí") { dialog, _ ->
                ratingsList.add(rating) // Almacena el rating
                updateAverageRatingDisplay() // Actualiza el promedio en el CardView
                Snackbar.make(findViewById(android.R.id.content), "Calificación de $rating estrellas guardada.", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                ratingBarApp.rating = 0f // Restablece el RatingBar si el usuario no guarda
                Snackbar.make(findViewById(android.R.id.content), "Calificación no guardada.", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setCancelable(false) // El usuario debe elegir sí o no
            .show()
    }

    private fun updateAverageRatingDisplay() {
        if (ratingsList.isNotEmpty()) {
            val average = ratingsList.sum() / ratingsList.size
            // Formatear a un decimal para que se vea bien
            textViewAverageRating.text = String.format("Promedio de Calificaciones: %.1f estrellas", average)
        } else {
            textViewAverageRating.text = "Promedio de Calificaciones: N/A"
        }
    }
}