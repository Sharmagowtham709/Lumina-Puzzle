package com.luminapuzzle.game

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

private val Application.dataStore by preferencesDataStore(name = "lumina_prefs")

data class PuzzleUiState(
    val size: Int = 5,
    val cells: List<Boolean> = List(25) { false },
    val moves: Int = 0,
    val level: Int = 1,
    val bestMoves: Int? = null,
    val isSolved: Boolean = false,
    val fastestWinStreak: Int = 0,
    val currentWinStreak: Int = 0,
    val totalWins: Int = 0,
    val dailyChallengeBest: Int? = null,
    val isDailyChallenge: Boolean = false,
    val showTutorial: Boolean = true,
    val tutorialCells: List<Boolean> = listOf(false, true, false, true, true, true, false, true, false),
    val hintsLeft: Int = 3,
    val hintCellIndex: Int? = null
)

class PuzzleGameViewModel(application: Application) : AndroidViewModel(application) {
    private val levelKey = intPreferencesKey("current_level")
    private val bestMovesKey = intPreferencesKey("best_moves")
    private val fastestWinStreakKey = intPreferencesKey("fastest_win_streak")
    private val currentWinStreakKey = intPreferencesKey("current_win_streak")
    private val totalWinsKey = intPreferencesKey("total_wins")
    private val dailyBestMovesKey = intPreferencesKey("daily_best_moves")
    private val dailyLastPlayedEpochKey = longPreferencesKey("daily_last_played_epoch")
    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()
    
    // Tracking solution taps to provide hints
    private var solutionTaps = mutableListOf<Int>()

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dataStore.data.first()
            val nowEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastDailyDay = prefs[dailyLastPlayedEpochKey]
            
            _uiState.value = _uiState.value.copy(
                level = prefs[levelKey] ?: 1,
                bestMoves = prefs[bestMovesKey],
                fastestWinStreak = prefs[fastestWinStreakKey] ?: 0,
                currentWinStreak = prefs[currentWinStreakKey] ?: 0,
                totalWins = prefs[totalWinsKey] ?: 0,
                dailyChallengeBest = if (lastDailyDay == nowEpochDay) prefs[dailyBestMovesKey] else null
            )
            startNewGame(level = _uiState.value.level)
        }
    }

    fun startNewGame(level: Int = _uiState.value.level, dailyChallenge: Boolean = false) {
        val size = _uiState.value.size
        solutionTaps.clear()
        val shuffled = if (dailyChallenge) {
            generateDailyChallengeBoard(size)
        } else {
            generateSolvableBoard(size, level)
        }
        _uiState.value = _uiState.value.copy(
            cells = shuffled,
            moves = 0,
            level = level,
            isSolved = shuffled.none { it },
            isDailyChallenge = dailyChallenge,
            hintsLeft = 3,
            hintCellIndex = null
        )
    }

    fun tapCell(index: Int) {
        val current = _uiState.value
        if (current.isSolved) return

        // Update solution state: if we tap a cell that was part of the solution, remove one instance of it.
        // If we tap a cell that was NOT part of the solution, add it to the required solution taps.
        // This works because toggling twice is the identity.
        if (solutionTaps.contains(index)) {
            solutionTaps.remove(index)
        } else {
            solutionTaps.add(index)
        }

        val nextCells = toggleCross(current.cells, current.size, index)
        val nextMoves = current.moves + 1
        val solved = nextCells.none { it }
        _uiState.value = current.copy(
            cells = nextCells,
            moves = nextMoves,
            isSolved = solved,
            hintCellIndex = null // Clear hint on any tap
        )

        if (solved) {
            viewModelScope.launch {
                updateBestScoreIfNeeded(nextMoves)
                updateAchievements(nextMoves)
            }
        }
    }

    fun nextLevel() {
        val nextLevel = _uiState.value.level + 1
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[levelKey] = nextLevel
            }
        }
        startNewGame(level = nextLevel)
    }

    fun startDailyChallenge() {
        startNewGame(level = _uiState.value.level, dailyChallenge = true)
    }

    fun dismissTutorial() {
        _uiState.value = _uiState.value.copy(showTutorial = false)
    }

    fun tapTutorialCell(index: Int) {
        val nextCells = toggleCross(_uiState.value.tutorialCells, 3, index)
        _uiState.value = _uiState.value.copy(tutorialCells = nextCells)
    }

    fun useHint() {
        val current = _uiState.value
        if (current.isSolved || current.hintsLeft <= 0 || solutionTaps.isEmpty()) return

        val hintIndex = solutionTaps.random()
        _uiState.value = current.copy(
            hintsLeft = current.hintsLeft - 1,
            hintCellIndex = hintIndex
        )
    }

    private suspend fun updateBestScoreIfNeeded(moves: Int) {
        val app = getApplication<Application>()
        val existingBest = app.dataStore.data.first()[bestMovesKey]
        if (existingBest == null || moves < existingBest) {
            app.dataStore.edit { prefs ->
                prefs[bestMovesKey] = moves
            }
            _uiState.value = _uiState.value.copy(bestMoves = moves)
        }
    }

    private suspend fun updateAchievements(moves: Int) {
        val app = getApplication<Application>()
        val prefs = app.dataStore.data.first()
        val totalWins = (prefs[totalWinsKey] ?: 0) + 1
        val currentStreak = (prefs[currentWinStreakKey] ?: 0) + 1
        val fastestStreak = maxOf(currentStreak, prefs[fastestWinStreakKey] ?: 0)
        val nowEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

        val isDailyChallenge = _uiState.value.isDailyChallenge
        var nextDailyBest = prefs[dailyBestMovesKey]
        val lastDailyDay = prefs[dailyLastPlayedEpochKey]

        if (isDailyChallenge) {
            if (lastDailyDay != nowEpochDay || nextDailyBest == null || moves < nextDailyBest) {
                nextDailyBest = moves
            }
        }

        app.dataStore.edit { editPrefs ->
            editPrefs[totalWinsKey] = totalWins
            editPrefs[currentWinStreakKey] = currentStreak
            editPrefs[fastestWinStreakKey] = fastestStreak
            if (isDailyChallenge) {
                editPrefs[dailyLastPlayedEpochKey] = nowEpochDay
                editPrefs[dailyBestMovesKey] = nextDailyBest!!
            }
        }

        _uiState.value = _uiState.value.copy(
            totalWins = totalWins,
            currentWinStreak = currentStreak,
            fastestWinStreak = fastestStreak,
            dailyChallengeBest = if (isDailyChallenge || lastDailyDay == nowEpochDay) nextDailyBest else null
        )
    }

    private fun generateSolvableBoard(size: Int, level: Int): List<Boolean> {
        val cellCount = size * size
        val taps = (size + level * 2).coerceAtMost(cellCount * 2)
        var board = List(cellCount) { false }

        repeat(taps) {
            val tapIndex = Random.nextInt(cellCount)
            if (solutionTaps.contains(tapIndex)) {
                solutionTaps.remove(tapIndex)
            } else {
                solutionTaps.add(tapIndex)
            }
            board = toggleCross(board, size, tapIndex)
        }
        return board
    }

    private fun generateDailyChallengeBoard(size: Int): List<Boolean> {
        val epochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val random = Random(epochDay)
        val cellCount = size * size
        val taps = size * 2 + 6
        var board = List(cellCount) { false }

        repeat(taps) {
            val tapIndex = random.nextInt(cellCount)
            if (solutionTaps.contains(tapIndex)) {
                solutionTaps.remove(tapIndex)
            } else {
                solutionTaps.add(tapIndex)
            }
            board = toggleCross(board, size, tapIndex)
        }
        return board
    }

    private fun toggleCross(cells: List<Boolean>, size: Int, index: Int): List<Boolean> {
        val row = index / size
        val col = index % size
        val changed = cells.toMutableList()

        fun toggleAt(r: Int, c: Int) {
            if (r in 0 until size && c in 0 until size) {
                val pos = r * size + c
                changed[pos] = !changed[pos]
            }
        }

        toggleAt(row, col)
        toggleAt(row - 1, col)
        toggleAt(row + 1, col)
        toggleAt(row, col - 1)
        toggleAt(row, col + 1)
        return changed
    }
}
