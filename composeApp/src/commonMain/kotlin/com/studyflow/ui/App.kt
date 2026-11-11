package com.studyflow.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.studyflow.AppContainer
import com.studyflow.model.StudyTask
import com.studyflow.util.nowMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Tab(val label: String) {
    TODAY("Today"),
    TIMETABLE("Timetable"),
    DEADLINES("Deadlines"),
    FOCUS("Focus"),
    ASSISTANT("Assistant"),
    ACHIEVEMENTS("Rewards"),
    SETTINGS("Settings"),
}

@Composable
fun App(container: AppContainer) {
    val settings by container.settings.state.collectAsState()
    val settingsLoaded by container.settings.loaded.collectAsState()

    StudyFlowTheme(
        themeMode = settings.themeMode,
        accentIndex = settings.accentIndex,
    ) {
        val repository = container.repository
        val data by repository.data.collectAsState()
        val loaded by repository.loaded.collectAsState()

        var now by remember { mutableStateOf(nowMillis()) }
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        val tabs = Tab.entries
        // Settings is opened with the gear on the Today screen (or the rail on wide screens)
        val navTabs = tabs.filter { it != Tab.SETTINGS }

        // Ticking a deadline plays the check animation first; the change is saved a moment later.
        val uiScope = rememberCoroutineScope()
        val toggleTask: (StudyTask, Boolean) -> Unit = { task, done ->
            uiScope.launch {
                delay(380)
                repository.setTaskDone(task.id, done)
            }
        }

        // keeps "in 2h 10m" labels fresh
        LaunchedEffect(Unit) {
            while (true) {
                delay(15_000)
                now = nowMillis()
            }
        }

        // optional background sync while the app is open
        LaunchedEffect(settings.autoSync, settings.syncHost) {
            if (settings.autoSync && settings.syncHost.isNotBlank()) {
                while (true) {
                    container.sync.syncNow()
                    delay(120_000)
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { container.sync.close() }
        }

        if (!loaded || !settingsLoaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@StudyFlowTheme
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 760.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!wide) {
                        NavigationBar {
                            navTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = tab.ordinal == tabIndex,
                                    onClick = { tabIndex = tab.ordinal },
                                    icon = { NavIcon(tab) },
                                    label = { Text(tab.label) },
                                    alwaysShowLabel = false,
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Row(Modifier.fillMaxSize()) {
                        if (wide) {
                            NavigationRail {
                                Spacer(Modifier.height(12.dp))
                                navTabs.forEach { tab ->
                                    NavigationRailItem(
                                        selected = tab.ordinal == tabIndex,
                                        onClick = { tabIndex = tab.ordinal },
                                        icon = { NavIcon(tab) },
                                        label = { Text(tab.label) },
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                NavigationRailItem(
                                    selected = tabIndex == Tab.SETTINGS.ordinal,
                                    onClick = { tabIndex = Tab.SETTINGS.ordinal },
                                    icon = { NavIcon(Tab.SETTINGS) },
                                    label = { Text(Tab.SETTINGS.label) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            AnimatedContent(
                                targetState = tabIndex,
                                transitionSpec = {
                                    val forward = targetState > initialState
                                    (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { w -> if (forward) w / 8 else -w / 8 }) togetherWith
                                        (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { w -> if (forward) -w / 8 else w / 8 })
                                },
                                label = "tabs",
                            ) { index ->
                                when (tabs[index]) {
                                    Tab.TODAY -> TodayScreen(
                                        data = data,
                                        now = now,
                                        userName = settings.userName,
                                        onOpenTab = { tabIndex = it.ordinal },
                                        onToggleTask = toggleTask,
                                        onEditTask = { tabIndex = Tab.DEADLINES.ordinal },
                                    )

                                    Tab.TIMETABLE -> TimetableScreen(
                                        data = data,
                                        onSave = repository::saveClass,
                                        onDelete = repository::deleteClass,
                                    )

                                    Tab.DEADLINES -> DeadlinesScreen(
                                        data = data,
                                        now = now,
                                        onSave = repository::saveTask,
                                        onToggle = toggleTask,
                                        onDelete = repository::deleteTask,
                                    )

                                    Tab.FOCUS -> FocusScreen(container = container, data = data, now = now)
                                    Tab.ASSISTANT -> AssistantScreen(
                                        container = container,
                                        onOpenSettings = { tabIndex = Tab.SETTINGS.ordinal },
                                    )

                                    Tab.ACHIEVEMENTS -> AchievementsScreen(
                                        data = data,
                                        now = now,
                                        userName = settings.userName,
                                    )

                                    Tab.SETTINGS -> SyncScreen(container = container, now = now)
                                }
                            }
                        }
                    }
                    CelebrationHost(repository)
                }
            }
        }
    }
}

/** Small hand-drawn icons so the app needs no icon libraries and looks the same everywhere. */
@Composable
fun NavIcon(tab: Tab) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tab) {
            Tab.TODAY -> {
                val house = Path().apply {
                    moveTo(w * 0.50f, h * 0.12f)
                    lineTo(w * 0.90f, h * 0.48f)
                    lineTo(w * 0.80f, h * 0.48f)
                    lineTo(w * 0.80f, h * 0.88f)
                    lineTo(w * 0.20f, h * 0.88f)
                    lineTo(w * 0.20f, h * 0.48f)
                    lineTo(w * 0.10f, h * 0.48f)
                    close()
                }
                drawPath(house, color, style = stroke)
            }

            Tab.TIMETABLE -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.20f),
                    size = Size(w * 0.72f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.14f, h * 0.42f), Offset(w * 0.86f, h * 0.42f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.34f, h * 0.10f), Offset(w * 0.34f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.66f, h * 0.10f), Offset(w * 0.66f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }

            Tab.DEADLINES -> {
                drawCircle(color, radius = w * 0.40f, center = Offset(w / 2, h / 2), style = stroke)
                val tick = Path().apply {
                    moveTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.45f, h * 0.65f)
                    lineTo(w * 0.69f, h * 0.38f)
                }
                drawPath(tick, color, style = stroke)
            }

            Tab.FOCUS -> {
                drawCircle(color, radius = w * 0.38f, center = Offset(w / 2, h * 0.56f), style = stroke)
                drawLine(color, Offset(w / 2, h * 0.56f), Offset(w / 2, h * 0.36f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(color, Offset(w / 2, h * 0.56f), Offset(w * 0.64f, h * 0.64f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.40f, h * 0.08f), Offset(w * 0.60f, h * 0.08f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }

            Tab.ASSISTANT -> {
                val spark = Path().apply {
                    moveTo(w * 0.50f, h * 0.06f)
                    quadraticTo(w * 0.50f, h * 0.50f, w * 0.94f, h * 0.50f)
                    quadraticTo(w * 0.50f, h * 0.50f, w * 0.50f, h * 0.94f)
                    quadraticTo(w * 0.50f, h * 0.50f, w * 0.06f, h * 0.50f)
                    quadraticTo(w * 0.50f, h * 0.50f, w * 0.50f, h * 0.06f)
                    close()
                }
                drawPath(spark, color, style = stroke)
                drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.80f, h * 0.20f))
            }

            Tab.ACHIEVEMENTS -> {
                val cup = Path().apply {
                    moveTo(w * 0.28f, h * 0.14f)
                    lineTo(w * 0.72f, h * 0.14f)
                    cubicTo(w * 0.72f, h * 0.50f, w * 0.62f, h * 0.60f, w * 0.50f, h * 0.60f)
                    cubicTo(w * 0.38f, h * 0.60f, w * 0.28f, h * 0.50f, w * 0.28f, h * 0.14f)
                    close()
                }
                drawPath(cup, color, style = stroke)
                drawArc(color, 90f, 180f, false, Offset(w * 0.08f, h * 0.18f), Size(w * 0.24f, h * 0.24f), style = stroke)
                drawArc(color, 270f, 180f, false, Offset(w * 0.68f, h * 0.18f), Size(w * 0.24f, h * 0.24f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.60f), Offset(w * 0.50f, h * 0.78f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.34f, h * 0.88f), Offset(w * 0.66f, h * 0.88f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }

            Tab.SETTINGS -> {
                val c = Offset(w / 2f, h / 2f)
                drawCircle(color, radius = w * 0.16f, center = c, style = stroke)
                drawCircle(color, radius = w * 0.33f, center = c, style = stroke)
                for (i in 0 until 8) {
                    val angle = i * (kotlin.math.PI / 4.0)
                    val dx = kotlin.math.cos(angle).toFloat()
                    val dy = kotlin.math.sin(angle).toFloat()
                    drawLine(
                        color,
                        Offset(c.x + dx * w * 0.33f, c.y + dy * w * 0.33f),
                        Offset(c.x + dx * w * 0.45f, c.y + dy * w * 0.45f),
                        strokeWidth = w * 0.10f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
