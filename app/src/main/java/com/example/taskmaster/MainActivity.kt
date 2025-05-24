package com.example.taskmaster

// Importaciones de Android y otras librerías necesarias
import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog // Para los diálogos de confirmación
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar // Para mensajes de usuario no intrusivos

// Importaciones para kotlinx.serialization y manejo de archivos
import kotlinx.serialization.encodeToString // Función para serializar objetos a String
import kotlinx.serialization.json.Json // Objeto principal para serialización/deserialización JSON
import kotlinx.serialization.decodeFromString // Función para deserializar String a objetos
import java.io.File // Para interactuar con archivos
import java.io.FileNotFoundException // Excepción para cuando un archivo no existe
import java.io.FileOutputStream // Para escribir datos en un archivo
import java.io.BufferedReader // Para leer datos eficientemente de un archivo
import java.io.InputStreamReader // Para convertir InputStream a Reader
import java.util.UUID // Para generar IDs únicos para las tareas

/**
 * MainActivity es el punto de entrada principal de la aplicación TaskMaster.
 * Contiene la lógica para la interfaz de usuario, interacción con widgets
 * y la persistencia de datos (guardar/cargar tareas en JSON).
 */
class MainActivity : AppCompatActivity() {

    // --- Declaración de Vistas (UI Widgets) ---
    // Se utiliza 'lateinit var' para indicar que estas variables se inicializarán
    // antes de ser usadas, típicamente en 'onCreate' con findViewById.
    private lateinit var editTextTaskName: EditText
    private lateinit var buttonAddTask: Button
    private lateinit var checkBoxTaskCompletedWidget: CheckBox // CheckBox general, no el del RecyclerView
    private lateinit var radioGroupPriority: RadioGroup
    private lateinit var progressBarProject: ProgressBar
    private lateinit var ratingBarApp: RatingBar
    private lateinit var spinnerTaskCategory: Spinner
    private lateinit var recyclerViewTasks: RecyclerView

    // Vistas para mostrar detalles de la tarea seleccionada en el TableLayout
    private lateinit var textViewSelectedTaskName: TextView
    private lateinit var textViewSelectedTaskPriority: TextView
    private lateinit var textViewSelectedTaskStatus: TextView

    // Vista para mostrar el promedio de calificaciones en el CardView
    private lateinit var textViewAverageRating: TextView

    // --- Variables de Datos y Lógica ---
    // Adaptador para el RecyclerView, manejará la visualización de la lista de tareas.
    private lateinit var taskAdapter: TaskAdapter
    // Lista mutable que contendrá los objetos 'Task'. 'mutableListOf' permite añadir/eliminar elementos.
    private val taskList = mutableListOf<Task>()
    // Lista para almacenar las calificaciones dadas a la aplicación.
    private val ratingsList = ArrayList<Float>()

    // Constante para el nombre del archivo JSON donde se guardarán las tareas.
    private val TASKS_FILE_NAME = "tasks.json"
    // Objeto JSON para serialización y deserialización.
    // 'prettyPrint = true' formatea el JSON para que sea legible en el archivo.
    private val json = Json { prettyPrint = true }

    /**
     * Se llama cuando la actividad es creada por primera vez.
     * Aquí se inicializan la mayoría de los componentes de la UI y se configuran listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece el layout de la actividad desde el archivo XML.
        setContentView(R.layout.activity_main)

        // Configuración de la barra de herramientas (Toolbar).
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.title_activity_main)

        // --- Inicialización de Vistas ---
        // Se asocian las variables de Kotlin con los elementos de la UI definidos en activity_main.xml
        editTextTaskName = findViewById(R.id.editTextTaskName)
        buttonAddTask = findViewById(R.id.buttonAddTask)
        checkBoxTaskCompletedWidget = findViewById(R.id.checkBoxTaskCompleted)
        radioGroupPriority = findViewById(R.id.radioGroupPriority)
        progressBarProject = findViewById(R.id.progressBarProject)
        ratingBarApp = findViewById(R.id.ratingBarApp)
        spinnerTaskCategory = findViewById(R.id.spinnerTaskCategory)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        textViewSelectedTaskName = findViewById(R.id.textViewSelectedTaskName)
        textViewSelectedTaskPriority = findViewById(R.id.textViewSelectedTaskPriority)
        textViewSelectedTaskStatus = findViewById(R.id.textViewSelectedTaskStatus)
        textViewAverageRating = findViewById(R.id.textViewAverageRating) // Inicializa el TextView del promedio

        // --- Configuración Inicial de Componentes ---
        setupSpinner() // Configura el Spinner con categorías.
        setupRecyclerView() // Configura el RecyclerView con el adaptador y el LayoutManager.

        // Intentar cargar tareas desde el archivo JSON al iniciar la aplicación.
        loadTasksFromJson()

        // Si no se cargaron tareas (lista vacía, archivo no existe o error), se cargan tareas de ejemplo.
        if (taskList.isEmpty()) {
            loadSampleTasks()
        }

        // Mostrar el promedio inicial de calificaciones (será N/A si no hay calificaciones guardadas).
        updateAverageRatingDisplay()

        // --- Configuración de Listeners (Manejo de Eventos de Usuario) ---

        // Listener para el botón "Añadir Tarea".
        buttonAddTask.setOnClickListener {
            addTaskFromInput() // Añade la tarea desde el input.
            saveTasksToJson() // Guarda las tareas después de añadir una nueva.
        }

        // Listener para el CheckBox general de "Tarea Completada".
        checkBoxTaskCompletedWidget.setOnCheckedChangeListener { _, isChecked ->
            // Proporciona feedback al usuario con un Snackbar.
            Snackbar.make(findViewById(android.R.id.content), "Checkbox general: ${if (isChecked) "Marcado" else "Desmarcado"}", Snackbar.LENGTH_SHORT).show()
        }

        // Listener para el RadioGroup de prioridad.
        radioGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            val selectedPriority = when (checkedId) {
                R.id.radioButtonHigh -> getString(R.string.high)
                R.id.radioButtonMedium -> getString(R.string.medium)
                R.id.radioButtonLow -> getString(R.string.low)
                else -> "N/A" // En caso de que no se seleccione nada (poco probable con un RadioGroup)
            }
            Snackbar.make(findViewById(android.R.id.content), "Prioridad seleccionada: $selectedPriority", Snackbar.LENGTH_SHORT).show()
        }

        // Listener para el RatingBar.
        // Se activa cuando el usuario cambia la calificación.
        ratingBarApp.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) { // Solo si el cambio fue por interacción del usuario
                showSaveRatingDialog(rating) // Muestra un diálogo para confirmar guardar la calificación.
            }
        }
    }

    /**
     * Configura el Spinner para la selección de categorías de tarea.
     */
    private fun setupSpinner() {
        val categories = arrayOf("Personal", "Trabajo", "Estudio", "Hogar", "Otro")
        // Crea un ArrayAdapter para vincular los datos (categorías) al Spinner.
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        // Define el layout para los elementos del menú desplegable del Spinner.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTaskCategory.adapter = adapter

        // Listener para detectar cuándo se selecciona un elemento en el Spinner.
        spinnerTaskCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                Snackbar.make(findViewById(android.R.id.content), "Categoría seleccionada: $selectedCategory", Snackbar.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No se realiza ninguna acción si no se selecciona nada.
            }
        }
    }

    /**
     * Configura el RecyclerView para mostrar la lista de tareas.
     */
    private fun setupRecyclerView() {
        // Inicializa el adaptador con la lista de tareas.
        taskAdapter = TaskAdapter(taskList)
        // Establece un LinearLayoutManager para que el RecyclerView muestre los elementos en una lista vertical.
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        // Asigna el adaptador al RecyclerView.
        recyclerViewTasks.adapter = taskAdapter

        // Configura los listeners personalizados definidos en el TaskAdapter.
        taskAdapter.setOnItemClickListener(object : TaskAdapter.OnItemClickListener {
            override fun onItemClick(task: Task) {
                Snackbar.make(findViewById(android.R.id.content), "Tarea clicada: ${task.name}", Snackbar.LENGTH_SHORT).show()
                displayTaskDetails(task) // Muestra los detalles de la tarea clicada en el TableLayout.
            }

            override fun onCheckboxClick(task: Task, isChecked: Boolean) {
                val status = if (isChecked) "completada" else "pendiente"
                Snackbar.make(findViewById(android.R.id.content), "${task.name} marcada como $status", Snackbar.LENGTH_SHORT).show()
                updateProgressBar() // Actualiza la barra de progreso cuando cambia el estado de una tarea.
                displayTaskDetails(task) // Actualiza los detalles si la tarea mostrada es la que se modificó.
                saveTasksToJson() // Guarda las tareas después de cambiar el estado.
            }

            override fun onDeleteClick(task: Task) {
                taskAdapter.removeTask(task) // Elimina la tarea de la lista y notifica al adaptador.
                Snackbar.make(findViewById(android.R.id.content), "Tarea '${task.name}' eliminada.", Snackbar.LENGTH_SHORT).show()
                updateProgressBar() // Actualiza la barra de progreso después de eliminar una tarea.
                // Si la tarea eliminada era la que se mostraba en detalles, limpiar los detalles.
                if (textViewSelectedTaskName.text.contains(task.name)) {
                    clearTaskDetails()
                }
                saveTasksToJson() // Guarda las tareas después de eliminar una.
            }
        })
    }

    /**
     * Carga tareas de ejemplo en la lista si no hay tareas guardadas.
     * Esta función se llama solo si 'loadTasksFromJson' no encuentra o no puede cargar datos.
     */
    private fun loadSampleTasks() {
        // Añade tareas de ejemplo a la lista. Se usa UUID.randomUUID().toString() para un ID único.
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_1_title), getString(R.string.sample_task_1_category), getString(R.string.medium), false))
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_2_title), getString(R.string.sample_task_2_category), getString(R.string.high), true))
        taskList.add(Task(UUID.randomUUID().toString(), getString(R.string.sample_task_3_title), getString(R.string.sample_task_3_category), getString(R.string.low), false))
        taskAdapter.notifyDataSetChanged() // Notifica al adaptador que los datos han cambiado para que se refresque la UI.
        updateProgressBar() // Actualiza la barra de progreso con las tareas de ejemplo.
    }

    /**
     * Añade una nueva tarea a la lista desde los campos de entrada de la UI.
     */
    private fun addTaskFromInput() {
        val taskName = editTextTaskName.text.toString().trim() // Obtiene el nombre de la tarea y elimina espacios en blanco.
        val selectedCategory = spinnerTaskCategory.selectedItem.toString() // Obtiene la categoría seleccionada del Spinner.

        // Obtiene la prioridad seleccionada del RadioGroup.
        val selectedPriorityId = radioGroupPriority.checkedRadioButtonId
        val priority: String = when (selectedPriorityId) {
            R.id.radioButtonHigh -> getString(R.string.high)
            R.id.radioButtonMedium -> getString(R.string.medium)
            R.id.radioButtonLow -> getString(R.string.low)
            else -> getString(R.string.medium) // Prioridad por defecto si ninguna está seleccionada.
        }

        if (taskName.isNotEmpty()) { // Verifica que el nombre de la tarea no esté vacío.
            // Crea un nuevo objeto Task.
            val newTask = Task(UUID.randomUUID().toString(), taskName, selectedCategory, priority)
            taskAdapter.addTask(newTask) // Añade la nueva tarea al adaptador y a la lista.
            editTextTaskName.text.clear() // Limpia el campo de entrada de texto.
            radioGroupPriority.check(R.id.radioButtonMedium) // Reinicia la prioridad a "Media".
            Snackbar.make(findViewById(android.R.id.content), "Tarea agregada: $taskName (Prioridad: $priority)", Snackbar.LENGTH_SHORT).show()
            updateProgressBar() // Actualiza la barra de progreso.
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Por favor, ingresa el nombre de la tarea.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra los detalles de una tarea específica en los TextViews del TableLayout.
     * @param task El objeto Task cuyos detalles se van a mostrar.
     */
    private fun displayTaskDetails(task: Task) {
        textViewSelectedTaskName.text = "${task.name}"
        textViewSelectedTaskPriority.text = "${task.priority}"
        val status = if (task.isCompleted) "Completada" else "Pendiente"
        textViewSelectedTaskStatus.text = "$status"
    }

    /**
     * Limpia los TextViews de detalles de la tarea, mostrando un mensaje de "No seleccionado".
     */
    private fun clearTaskDetails() {
        textViewSelectedTaskName.text = "No seleccionado"
        textViewSelectedTaskPriority.text = "No seleccionado"
        textViewSelectedTaskStatus.text = "No seleccionado"
    }

    /**
     * Actualiza el progreso de la barra de progreso basada en el número de tareas completadas.
     */
    private fun updateProgressBar() {
        if (taskList.isEmpty()) { // Si no hay tareas, el progreso es 0.
            progressBarProject.progress = 0
            return
        }

        // Calcula el número de tareas completadas y el total.
        val completedTasksCount = taskList.count { it.isCompleted }
        val totalTasksCount = taskList.size
        // Calcula el porcentaje de progreso.
        val progress = (completedTasksCount.toDouble() / totalTasksCount * 100).toInt()
        progressBarProject.progress = progress // Actualiza la barra de progreso.
    }

    // --- Funciones para la Persistencia de Datos (JSON) y RatingBar ---

    /**
     * Muestra un diálogo de confirmación para guardar la calificación del RatingBar.
     * @param rating La calificación que el usuario ha seleccionado.
     */
    private fun showSaveRatingDialog(rating: Float) {
        AlertDialog.Builder(this)
            .setTitle("Guardar Calificación") // Título del diálogo
            .setMessage("¿Deseas guardar tu calificación de $rating estrellas?") // Mensaje
            .setPositiveButton("Sí") { dialog, _ -> // Botón "Sí"
                ratingsList.add(rating) // Añade la calificación a la lista.
                updateAverageRatingDisplay() // Recalcula y muestra el promedio.
                Snackbar.make(findViewById(android.R.id.content), "Calificación de $rating estrellas guardada.", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss() // Cierra el diálogo.
            }
            .setNegativeButton("No") { dialog, _ -> // Botón "No"
                ratingBarApp.rating = 0f // Reinicia el RatingBar a 0 si no se guarda.
                Snackbar.make(findViewById(android.R.id.content), "Calificación no guardada.", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss() // Cierra el diálogo.
            }
            .setCancelable(false) // Hace que el diálogo no se pueda cerrar tocando fuera de él.
            .show() // Muestra el diálogo.
    }

    /**
     * Calcula el promedio de las calificaciones guardadas y lo muestra en el TextView correspondiente.
     */
    private fun updateAverageRatingDisplay() {
        if (ratingsList.isNotEmpty()) { // Si hay calificaciones
            val average = ratingsList.sum() / ratingsList.size // Calcula el promedio.
            // Formatea el promedio a un decimal y lo muestra.
            textViewAverageRating.text = String.format("Promedio de Calificaciones: %.1f estrellas", average)
        } else { // Si no hay calificaciones
            textViewAverageRating.text = "Promedio de Calificaciones: N/A" // Muestra "N/A".
        }
    }

    /**
     * Guarda la lista actual de tareas en un archivo JSON en el almacenamiento interno de la aplicación.
     */
    private fun saveTasksToJson() {
        try {
            // Serializa la lista de objetos 'Task' a una cadena JSON.
            val jsonString = json.encodeToString(taskList)

            // Crea un objeto File que apunta al archivo 'tasks.json' en el directorio de archivos interno.
            val file = File(filesDir, TASKS_FILE_NAME)

            // Abre un FileOutputStream para escribir en el archivo. 'use' asegura el cierre automático.
            FileOutputStream(file).use {
                it.write(jsonString.toByteArray()) // Escribe la cadena JSON convertida a bytes.
            }
            Snackbar.make(findViewById(android.R.id.content), "Tareas guardadas en JSON.", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Captura cualquier excepción que pueda ocurrir durante el guardado (ej. error de escritura).
            e.printStackTrace() // Imprime la traza del error para depuración.
            Snackbar.make(findViewById(android.R.id.content), "Error al guardar tareas: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Carga las tareas desde un archivo JSON del almacenamiento interno de la aplicación.
     * Si el archivo no existe o hay un error, maneja la situación adecuadamente.
     */
    private fun loadTasksFromJson() {
        try {
            val file = File(filesDir, TASKS_FILE_NAME)

            // Verifica si el archivo de tareas existe.
            if (!file.exists()) {
                Snackbar.make(findViewById(android.R.id.content), "No se encontraron tareas guardadas (primera ejecución).", Snackbar.LENGTH_SHORT).show()
                return // Sale de la función si el archivo no existe.
            }

            // Abre un InputStream para leer el archivo.
            val inputStream = openFileInput(TASKS_FILE_NAME)
            // Usa BufferedReader para leer el contenido del archivo eficientemente como texto.
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() } // Lee todo el contenido del archivo a una cadena.

            // Deserializa la cadena JSON de nuevo a una lista de objetos 'Task'.
            val loadedTasks = json.decodeFromString<List<Task>>(jsonString)

            // Limpia la lista actual de tareas y añade todas las tareas cargadas.
            taskList.clear()
            taskList.addAll(loadedTasks)
            taskAdapter.notifyDataSetChanged() // Notifica al adaptador del RecyclerView para que se actualice la UI.
            updateProgressBar() // Actualiza la barra de progreso con las tareas cargadas.

            Snackbar.make(findViewById(android.R.id.content), "Tareas cargadas desde JSON.", Snackbar.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            // Esta excepción ocurre si el archivo no se encuentra. Es normal en la primera ejecución.
            Snackbar.make(findViewById(android.R.id.content), "Archivo de tareas no encontrado.", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Captura cualquier otra excepción durante la carga (ej. JSON mal formado, errores de lectura).
            e.printStackTrace() // Imprime la traza del error para depuración.
            Snackbar.make(findViewById(android.R.id.content), "Error al cargar tareas: ${e.message}", Snackbar.LENGTH_LONG).show()
            // Si hay un error, es una buena práctica limpiar la lista para evitar trabajar con datos corruptos.
            taskList.clear()
            taskAdapter.notifyDataSetChanged()
            updateProgressBar()
        }
    }
}