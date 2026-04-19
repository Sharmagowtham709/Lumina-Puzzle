package com.luminapuzzle.game

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private val viewModel: PuzzleGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0E24)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0F0E24),
                                        Color(0xFF1E1C44),
                                        Color(0xFF0F0E24)
                                    )
                                )
                            )
                    ) {
                        PuzzleGameScreen(viewModel = viewModel)
                    }
                }

                if (state.showTutorial) {
                    TutorialOverlay(
                        cells = state.tutorialCells,
                        onTap = viewModel::tapTutorialCell,
                        onDismiss = viewModel::dismissTutorial
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(
    cells: List<Boolean>,
    onTap: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1E1C44),
            border = BorderStroke(2.dp, Color(0xFF8EE6FF))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.how_to_play),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(id = R.string.tutorial_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB6F0FF),
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    LightsGrid(
                        cells = cells,
                        size = 3,
                        onTap = onTap
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(id = R.string.skip_tutorial),
                            color = Color(0xFF8D90A0)
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.got_it))
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleGameScreen(viewModel: PuzzleGameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.title).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF8EE6FF),
                    letterSpacing = 4.sp
                )
                Text(
                    text = stringResource(id = R.string.subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFB6F0FF).copy(alpha = 0.7f)
                )
            }

            StatsCard(
                level = state.level,
                moves = state.moves,
                bestMoves = state.bestMoves,
                totalWins = state.totalWins,
                streak = state.currentWinStreak,
                dailyBest = state.dailyChallengeBest,
                isDaily = state.isDailyChallenge
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LightsGrid(
                    cells = state.cells,
                    size = state.size,
                    hintIndex = state.hintCellIndex,
                    onTap = { index ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                        viewModel.tapCell(index)
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isSolved) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.puzzle_completed).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::nextLevel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8EE6FF), contentColor = Color(0xFF0F0E24))
                        ) {
                            Text(text = stringResource(id = R.string.next_level), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startNewGame() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2959)),
                            border = BorderStroke(1.dp, Color(0xFF3E3A8C))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(text = stringResource(id = R.string.new_game))
                        }
                        
                        Button(
                            onClick = viewModel::useHint,
                            modifier = Modifier.weight(1f).height(56.dp),
                            enabled = state.hintsLeft > 0,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE91E63),
                                disabledContainerColor = Color(0xFF2E2E3A)
                            )
                        ) {
                            Text(text = "${stringResource(id = R.string.hint_btn)} (${state.hintsLeft})")
                        }
                    }

                    Button(
                        onClick = viewModel::startDailyChallenge,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isDailyChallenge) Color(0xFFFFC107) else Color(0xFF1E1C44)
                        ),
                        border = BorderStroke(1.dp, if (state.isDailyChallenge) Color(0xFFFFD54F) else Color(0xFF3E3A8C))
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(text = stringResource(id = R.string.daily_challenge))
                    }
                }

                AchievementStrip(
                    totalWins = state.totalWins,
                    streak = state.currentWinStreak,
                    fastestStreak = state.fastestWinStreak
                )

                AdMobBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LightsGrid(cells: List<Boolean>, size: Int, hintIndex: Int? = null, onTap: (Int) -> Unit) {
    BoxWithConstraints {
        val spacing = if (maxWidth > 600.dp) 12.dp else 8.dp
        val tileSize = computeTileSize(maxWidth, size, spacing)
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(size) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(size) { col ->
                        val index = row * size + col
                        val isOn = cells[index]
                        val isHint = index == hintIndex
                        
                        val cellColor by animateColorAsState(
                            targetValue = if (isOn) Color(0xFF8EE6FF) else Color(0xFF2A2959).copy(alpha = 0.8f),
                            animationSpec = tween(300),
                            label = "color"
                        )
                        
                        val scale by animateFloatAsState(
                            targetValue = if (isOn) 1.05f else 1f,
                            label = "scale"
                        )

                        val infiniteTransition = rememberInfiniteTransition(label = "hint")
                        val borderAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(tileSize)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(14.dp))
                                .background(cellColor)
                                .then(
                                    if (isHint) Modifier.border(
                                        width = 3.dp,
                                        color = Color(0xFFFFEB3B).copy(alpha = borderAlpha),
                                        shape = RoundedCornerShape(14.dp)
                                    ) else if (isOn) Modifier.border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) else Modifier
                                )
                                .clickable { onTap(index) }
                        )
                    }
                }
            }
        }
    }
}

private fun computeTileSize(maxWidth: Dp, size: Int, spacing: Dp): Dp {
    val totalSpacing = spacing * (size - 1)
    val maxTile = (maxWidth - totalSpacing) / size
    return maxTile.coerceIn(44.dp, 88.dp)
}

@Composable
private fun StatsCard(
    level: Int,
    moves: Int,
    bestMoves: Int?,
    totalWins: Int,
    streak: Int,
    dailyBest: Int?,
    isDaily: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C44).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF3E3A8C).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = if (isDaily) stringResource(id = R.string.daily_challenge) else stringResource(id = R.string.level, level),
                        color = Color(0xFF8EE6FF),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.moves, moves),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Surface(
                    color = Color(0xFF2A2959),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E3A8C))
                ) {
                    Text(
                        text = bestMoves?.let { stringResource(id = R.string.best_score, it) } ?: stringResource(id = R.string.best_score_empty),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFFB6F0FF),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3E3A8C).copy(alpha = 0.3f)))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = stringResource(id = R.string.total_wins, totalWins), icon = Icons.Default.EmojiEvents)
                StatItem(label = stringResource(id = R.string.streak, streak), icon = Icons.Default.Star)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(4.dp))
        Text(text = label, color = Color(0xFFB6F0FF), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AchievementStrip(totalWins: Int, streak: Int, fastestStreak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AchievementChip(label = stringResource(id = R.string.achievement_novice), unlocked = totalWins >= 3)
        AchievementChip(label = stringResource(id = R.string.achievement_rising), unlocked = totalWins >= 10)
        AchievementChip(label = stringResource(id = R.string.achievement_streak), unlocked = streak >= 3 || fastestStreak >= 3)
    }
}

@Composable
private fun AchievementChip(label: String, unlocked: Boolean) {
    val alpha by animateFloatAsState(if (unlocked) 1f else 0.4f, label = "alpha")
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color(0xFF2A5B6A).copy(alpha = 0.8f) else Color(0xFF2E2E3A)
        ),
        modifier = Modifier
            .defaultMinSize(minWidth = 90.dp)
            .graphicsLayer(alpha = alpha),
        shape = RoundedCornerShape(12.dp),
        border = if (unlocked) BorderStroke(1.dp, Color(0xFF8EE6FF).copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (unlocked) Color(0xFFE1FBFF) else Color(0xFF8D90A0),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.height(52.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
