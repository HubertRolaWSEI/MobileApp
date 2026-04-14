package pl.wsei.pam.lab06

import android.Manifest
import android.app.*
import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.accompanist.permissions.*
import kotlinx.coroutines.launch
import pl.wsei.pam.lab06.data.AppContainer
import pl.wsei.pam.lab06.data.LocalDateConverter
import pl.wsei.pam.lab06.viewmodel.*
import java.time.LocalDate

// --- STAŁE I MODELE ---
const val notificationID = 121
const val channelID = "Lab06 channel"
const val titleExtra = "title"
const val messageExtra = "message"

enum class Priority { High, Medium, Low }

data class TodoTask(
    val id: Int = 0,
    val title: String = "",
    val deadline: LocalDate = LocalDate.now(),
    var isDone: Boolean = false,
    val priority: Priority = Priority.Low
)

// --- AKTYWNOŚĆ ---
class Lab06Activity : ComponentActivity() {
    companion object { lateinit var container: AppContainer }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelID, "Zadania", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun scheduleAlarm(time: Long) {
        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java).apply {
            putExtra(titleExtra, "Deadline")
            putExtra(messageExtra, "Zbliża się termin zakończenia zadania")
        }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, notificationID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    fun cancelAlarm() {
        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, notificationID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        }
    }

    fun scheduleAlarmForClosestTask(tasks: List<TodoTask>) {
        val now = LocalDate.now()
        val undoneFutureTasks = tasks.filter { !it.isDone && it.deadline.isAfter(now) }
        if (undoneFutureTasks.isEmpty()) { cancelAlarm(); return }

        val closest = undoneFutureTasks.minByOrNull { it.deadline }!!
        val alarmMillis = LocalDateConverter.toMillis(closest.deadline.minusDays(1))
        val finalTime = if (alarmMillis < System.currentTimeMillis()) System.currentTimeMillis() + 2000L else alarmMillis

        cancelAlarm()
        scheduleAlarm(finalTime)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        container = (application as TodoApplication).container
        setContent { MaterialTheme { Surface { MainScreen(this@Lab06Activity) } } }
    }
}

// --- EKRANY ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(activity: Lab06Activity) {
    val navController = rememberNavController()
    val permission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) { if (!permission.status.isGranted) permission.launchPermissionRequest() }

    NavHost(navController, "list") {
        composable("list") { ListScreen(navController, activity) }
        composable("form") { FormScreen(navController) }
        composable("form/{taskId}", arguments = listOf(navArgument("taskId") { type = NavType.IntType })) {
            FormScreen(navController, it.arguments?.getInt("taskId") ?: 0)
        }
    }
}

@Composable
fun ListScreen(navController: NavController, activity: Lab06Activity, viewModel: ListViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.listUiState.collectAsState()
    LaunchedEffect(state.items) { activity.scheduleAlarmForClosestTask(state.items) }

    Scaffold(
        topBar = { AppTopBar(navController, "Lista zadań", false, "form") },
        floatingActionButton = {
            FloatingActionButton(shape = CircleShape, onClick = { navController.navigate("form") }) {
                Icon(Icons.Filled.Add, "Dodaj", modifier = Modifier.scale(1.5f))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(state.items, key = { it.id }) { task ->
                ListItem(task, { navController.navigate("form/${task.id}") }, { viewModel.deleteTask(task) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(navController: NavController, taskId: Int = 0, viewModel: FormViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(taskId) { if (taskId > 0) viewModel.loadTask(taskId) }

    Scaffold(topBar = {
        AppTopBar(navController, if (viewModel.isEditMode) "Edytuj zadanie" else "Dodaj zadanie", true, "list", onSaveClick = {
            if (viewModel.todoTaskUiState.isValid) scope.launch { viewModel.save(); navController.popBackStack() }
        })
    }) { padding -> TodoTaskInputBody(viewModel.todoTaskUiState, viewModel::updateUiState, Modifier.padding(padding)) }
}

// --- KOMPONENTY FORMULARZA ---
@Composable
fun TodoTaskInputBody(uiState: TodoTaskUiState, onValueChange: (TodoTaskForm) -> Unit, modifier: Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TodoTaskInputForm(uiState.todoTask, onValueChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTaskInputForm(item: TodoTaskForm, onValueChange: (TodoTaskForm) -> Unit) {
    Text("Tytuł zadania")
    TextField(value = item.title, onValueChange = { onValueChange(item.copy(title = it)) }, Modifier.fillMaxWidth())

    var showDialog by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = item.deadline)
    Text("Data: ${LocalDateConverter.fromMillis(item.deadline)}", Modifier.clickable { showDialog = true }.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)

    if (showDialog) {
        DatePickerDialog(onDismissRequest = { showDialog = false }, confirmButton = {
            Button(onClick = { showDialog = false; onValueChange(item.copy(deadline = dateState.selectedDateMillis ?: item.deadline)) }) { Text("OK") }
        }) { DatePicker(dateState) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Wykonane", Modifier.weight(1f))
        Switch(item.isDone, { onValueChange(item.copy(isDone = it)) })
    }

    Text("Priorytet")
    Priority.values().forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onValueChange(item.copy(priority = p.name)) }) {
            RadioButton(item.priority == p.name, { onValueChange(item.copy(priority = p.name)) })
            Text(p.name)
        }
    }
}

// --- ELEMENT LISTY (Z PRIORYTETEM) ---
@Composable
fun ListItem(item: TodoTask, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.padding(8.dp).fillMaxWidth().clickable { onEdit() }) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Termin: ${item.deadline}", fontSize = 14.sp)

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
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Usuń", tint = Color.Red) }
        }
    }
}

// --- PASEK GÓRNY (Z IKONĄ HOME) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, title: String, showBackIcon: Boolean, route: String, onSaveClick: () -> Unit = {}) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = { Text(title) },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Wróć") }
            }
        },
        actions = {
            if (showBackIcon) {
                OutlinedButton(onClick = onSaveClick) { Text("Zapisz", fontSize = 18.sp) }
            } else {
                IconButton(onClick = {
                    Lab06Activity.container.notificationHandler.showSimpleNotification()
                }) {
                    Icon(Icons.Default.Settings, "Ustawienia")
                }
                IconButton(onClick = { /* Akcja Home */ }) {
                    Icon(Icons.Default.Home, "Home")
                }
            }
        }
    )
}