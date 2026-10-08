package com.example.social_media_tracker_app.domain.pomodoro

import com.example.social_media_tracker_app.data.repository.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PomodoroSessionType {
    FOCUS,
    BREAK
}

sealed class PomodoroEvent {
    data class BreakStarted(val completedCycle: Int, val totalCycles: Int, val breakMinutes: Int) : PomodoroEvent()
    data class FocusStarted(val currentCycle: Int, val totalCycles: Int, val focusMinutes: Int) : PomodoroEvent()
    data class AllCompleted(val totalCycles: Int) : PomodoroEvent()
}

data class PomodoroState(
    val isRunning: Boolean = false,
    val sessionType: PomodoroSessionType = PomodoroSessionType.FOCUS,
    val totalCycles: Int = 2,
    val currentCycle: Int = 1,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val focusMode: String = "music" // "music" | "study"
) {
    val isFocusSession: Boolean
        get() = isRunning && sessionType == PomodoroSessionType.FOCUS

    val isBreakSession: Boolean
        get() = isRunning && sessionType == PomodoroSessionType.BREAK

    val formattedRemainingTime: String
        get() {
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return "%02d:%02d".format(m, s)
        }

    val statusLabel: String
        get() {
            if (!isRunning) return ""
            return if (sessionType == PomodoroSessionType.FOCUS) {
                val modeIcon = if (focusMode == "study") "📚" else "🎵"
                "$modeIcon $formattedRemainingTime ($currentCycle/$totalCycles)"
            } else {
                "☕ $formattedRemainingTime (Nghỉ $currentCycle/$totalCycles)"
            }
        }
}

class PomodoroManager(
    private val repository: StatsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PomodoroEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PomodoroEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null

    init {
        scope.launch {
            repository.appConfig.collect { config ->
                if (!config.pomodoro.enabled && _state.value.isRunning) {
                    stop()
                }
            }
        }
    }

    fun startNewSession(
        totalCycles: Int = 2,
        focusMinutes: Int = 25,
        breakMinutes: Int = 5,
        focusMode: String = "music"
    ) {
        val totalSec = focusMinutes * 60
        _state.value = PomodoroState(
            isRunning = true,
            sessionType = PomodoroSessionType.FOCUS,
            totalCycles = maxOf(1, totalCycles),
            currentCycle = 1,
            focusMinutes = focusMinutes,
            breakMinutes = breakMinutes,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            focusMode = focusMode
        )

        runTimer()
        _events.tryEmit(PomodoroEvent.FocusStarted(1, maxOf(1, totalCycles), focusMinutes))
    }

    fun toggle() {
        if (_state.value.isRunning) {
            pause()
        } else {
            if (_state.value.remainingSeconds <= 0) {
                val initialFocus = _state.value.focusMinutes * 60
                _state.value = _state.value.copy(remainingSeconds = initialFocus, totalSeconds = initialFocus)
            }
            start()
        }
    }

    fun start() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)
        runTimer()
    }

    fun pause() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun stop() {
        timerJob?.cancel()
        val initialFocus = _state.value.focusMinutes * 60
        _state.value = _state.value.copy(
            isRunning = false,
            sessionType = PomodoroSessionType.FOCUS,
            currentCycle = 1,
            remainingSeconds = initialFocus,
            totalSeconds = initialFocus
        )
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _state.value.isRunning) {
                delay(1000L)
                val currentRemaining = _state.value.remainingSeconds - 1
                if (currentRemaining <= 0) {
                    onSessionCompleted()
                } else {
                    _state.value = _state.value.copy(remainingSeconds = currentRemaining)
                }
            }
        }
    }

    private fun onSessionCompleted() {
        val current = _state.value
        if (current.sessionType == PomodoroSessionType.FOCUS) {
            // Thưởng +15⚡ khi hoàn thành mỗi hiệp Focus
            repository.rewardPet(15, "pomodoro_complete")

            if (current.currentCycle < current.totalCycles) {
                // Chuyển sang hiệp Nghỉ giải lao (Break)
                val breakSec = current.breakMinutes * 60
                _state.value = current.copy(
                    sessionType = PomodoroSessionType.BREAK,
                    remainingSeconds = breakSec,
                    totalSeconds = breakSec
                )
                _events.tryEmit(PomodoroEvent.BreakStarted(current.currentCycle, current.totalCycles, current.breakMinutes))
            } else {
                // Đã hoàn thành toàn bộ tất cả các hiệp! Thưởng thêm +10⚡ vinh danh
                repository.rewardPet(10, "pomodoro_all_cycles_complete")
                val completedCycles = current.totalCycles
                stop()
                _events.tryEmit(PomodoroEvent.AllCompleted(completedCycles))
            }
        } else {
            // Vừa hết giờ Break -> Bắt đầu hiệp Focus kế tiếp
            val nextCycle = current.currentCycle + 1
            val focusSec = current.focusMinutes * 60
            _state.value = current.copy(
                sessionType = PomodoroSessionType.FOCUS,
                currentCycle = nextCycle,
                remainingSeconds = focusSec,
                totalSeconds = focusSec
            )
            _events.tryEmit(PomodoroEvent.FocusStarted(nextCycle, current.totalCycles, current.focusMinutes))
        }
    }
}
