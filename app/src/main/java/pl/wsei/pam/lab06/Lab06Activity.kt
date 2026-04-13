package pl.wsei.pam.lab06

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import pl.wsei.pam.lab06.data.AppContainer
import pl.wsei.pam.lab06.data.LocalDateConverter
import pl.wsei.pam.lab06.viewmodel.*
import java.time.LocalDate

// --- STAŁE ---

const val notificationID = 121
const val channelID = "Lab06 channel"
const val titleExtra = "title"
const val messageExtra = "message"

// --- MODELE DANYCH ---

enum class Priority {
    High, Medium, Low
}

data class TodoTask(
    val id: Int = 0,
    val title: String = "",
    val deadline: LocalDate = LocalDate.now(),
    var isDone: Boolean = false,
    val priority: Priority = Priority.Low
)

// --- AKTYWNOŚĆ ---

class Lab06Activity : ComponentActivity() {

    companion object {
        lateinit var container: AppContainer
    }

    private fun createNotificationChannel() {
        val name = "Lab06 channel"
        val descriptionText = "Lab06 is channel for notifications for approaching tasks."
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleAlarm(time: Long) {
        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java)
        intent.putExtra(titleExtra, "Deadline")
        intent.putExtra(messageExtra, "Zbliża się termin zakończenia zadania")

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }

    fun cancelAlarm() {
        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleAlarmForClosestTask(tasks: List<TodoTask>) {
        val now = LocalDate.now()
        val undoneFutureTasks = tasks.filter { !it.isDone && it.deadline.isAfter(now) }

        if (undoneFutureTasks.isEmpty()) {
            cancelAlarm()
            return
        }

        val closest = undoneFutureTasks.minByOrNull { it.deadline } ?: return
        val alarmDate = closest.deadline.minusDays(1)
        val alarmMillis = LocalDateConverter.toMillis(alarmDate)
        val nowMillis = System.currentTimeMillis()
        val finalTime = if (alarmMillis < nowMillis) nowMillis + 2000L else alarmMillis

        cancelAlarm()
        scheduleAlarm(finalTime)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        container = (this.application as TodoApplication).container

        // MODYFIKACJA: Wywołanie testowe alarmu na 2 sekundy od teraz
        scheduleAlarm(System.currentTimeMillis() + 2000L)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(activity = this@Lab06Activity)
                }
            }
        }
    }
}

// --- GŁÓWNY EKRAN I NAWIGACJA ---

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(activity: Lab06Activity? = null) {
    val navController = rememberNavController()

    val postNotificationPermission =
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(key1 = true) {
        if (!postNotificationPermission.status.isGranted) {
            postNotificationPermission.launchPermissionRequest()
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") { ListScreen(navController = navController, activity = activity) }
        composable("form") { FormScreen(navController = navController, activity = activity) }
        composable(
            "form/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
            FormScreen(navController = navController, activity = activity, taskId = taskId)
        }
    }
}

// --- EKRAN LISTY ---

@Composable
fun ListScreen(
    navController: NavController,
    activity: Lab06Activity? = null,
    viewModel: ListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val listUiState by viewModel.listUiState.collectAsState()

    LaunchedEffect(listUiState.items) {
        activity?.scheduleAlarmForClosestTask(listUiState.items)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = "Lista zadań",
                showBackIcon = false,
                route = "form"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                shape = CircleShape,
                onClick = { navController.navigate("form") },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Dodaj zadanie",
                        modifier = Modifier.scale(1.5f)
                    )
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(items = listUiState.items, key = { it.id }) { task ->
                    ListItem(
                        item = task,
                        onEdit = { navController.navigate("form/${task.id}") },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    )
}

// --- EKRAN FORMULARZA ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    navController: NavController,
    activity: Lab06Activity? = null,
    taskId: Int = 0,
    viewModel: FormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(taskId) {
        if (taskId > 0) {
            viewModel.loadTask(taskId)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = if (viewModel.isEditMode) "Edytuj zadanie" else "Dodaj zadanie",
                showBackIcon = true,
                route = "list",
                onSaveClick = {
                    if (viewModel.todoTaskUiState.isValid) {
                        coroutineScope.launch {
                            viewModel.save()
                            navController.navigate("list") {
                                popUpTo("list") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    ) {
        TodoTaskInputBody(
            todoUiState = viewModel.todoTaskUiState,
            onItemValueChange = viewModel::updateUiState,
            modifier = Modifier.padding(it)
        )
    }
}

// --- BODY FORMULARZA ---

@Composable
fun TodoTaskInputBody(
    todoUiState: TodoTaskUiState,
    onItemValueChange: (TodoTaskForm) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TodoTaskInputForm(
            item = todoUiState.todoTask,
            onValueChange = onItemValueChange,
            modifier = modifier
        )
    }
}

// --- FORMULARZ Z POLAMI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTaskInputForm(
    item: TodoTaskForm,
    modifier: Modifier = Modifier,
    onValueChange: (TodoTaskForm) -> Unit = {},
    enabled: Boolean = true
) {
    Text("Tytuł zadania")
    TextField(
        value = item.title,
        onValueChange = {
            onValueChange(item.copy(title = it))
        }
    )

    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker,
        yearRange = IntRange(2000, 2030),
        initialSelectedDateMillis = item.deadline
    )
    var showDialog by remember { mutableStateOf(false) }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { showDialog = true }),
        text = "Date",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    onValueChange(item.copy(deadline = datePickerState.selectedDateMillis!!))
                }) {
                    Text("Pick")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = true)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Wykonane", modifier = Modifier.weight(1f))
        Switch(
            checked = item.isDone,
            onCheckedChange = {
                onValueChange(item.copy(isDone = it))
            }
        )
    }

    Text("Priorytet")
    Column {
        Priority.values().forEach { priorityOption ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(item.copy(priority = priorityOption.name)) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = item.priority == priorityOption.name,
                    onClick = { onValueChange(item.copy(priority = priorityOption.name)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = priorityOption.name)
            }
        }
    }
}

// --- KOMPONENT ELEMENTU LISTY ---

@Composable
fun ListItem(
    item: TodoTask,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Termin: ${item.deadline}", fontSize = 14.sp)

                val priorityColor = when (item.priority) {
                    Priority.High -> Color.Red
                    Priority.Medium -> Color(0xFFFFA500)
                    Priority.Low -> Color(0xFF009900)
                }
                Text(
                    text = "Priorytet: ${item.priority.name}",
                    fontSize = 14.sp,
                    color = priorityColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (item.isDone) "Wykonane ✓" else "Do zrobienia",
                    fontSize = 14.sp,
                    color = if (item.isDone) Color(0xFF009900) else Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = Color.Red
                )
            }
        }
    }
}

// --- TOP BAR ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavController,
    title: String,
    showBackIcon: Boolean,
    route: String,
    onSaveClick: () -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = { Text(text = title) },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(onClick = { navController.navigate(route) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Wróć")
                }
            }
        },
        actions = {
            if (route != "form") {
                OutlinedButton(onClick = onSaveClick) {
                    Text(text = "Zapisz", fontSize = 18.sp)
                }
            } else {
                IconButton(onClick = {
                    Lab06Activity.container.notificationHandler.showSimpleNotification()
                }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Ustawienia")
                }
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                }
            }
        }
    )
}