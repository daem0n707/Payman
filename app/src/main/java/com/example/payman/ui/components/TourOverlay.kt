package com.example.payman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class TourStep(
    val id: String,
    val title: String,
    val description: String,
    val screen: String,
    val onNext: (() -> Unit)? = null
)

class TourState {
    var currentStepIndex by mutableStateOf(-1)
    val steps = mutableStateListOf<TourStep>()
    val targets = mutableStateMapOf<String, Pair<Offset, Size>>()
    var isVisible by mutableStateOf(false)

    fun startTour(tourSteps: List<TourStep>) {
        steps.clear()
        steps.addAll(tourSteps)
        currentStepIndex = 0
        isVisible = true
    }

    fun nextStep() {
        if (currentStepIndex < steps.size - 1) {
            steps[currentStepIndex].onNext?.invoke()
            currentStepIndex++
        } else {
            isVisible = false
            currentStepIndex = -1
        }
    }

    fun skipTour() {
        isVisible = false
        currentStepIndex = -1
    }

    val currentStep: TourStep?
        get() = if (currentStepIndex in steps.indices) steps[currentStepIndex] else null
}

@Composable
fun rememberTourState() = remember { TourState() }

fun Modifier.tourTarget(state: TourState, stepId: String): Modifier = this.onGloballyPositioned { coords ->
    if (coords.isAttached) {
        state.targets[stepId] = coords.positionInWindow() to Size(coords.size.width.toFloat(), coords.size.height.toFloat())
    }
}

@Composable
fun TourHost(state: TourState, currentScreen: String) {
    if (!state.isVisible || state.currentStep == null) return
    
    val step = state.currentStep!!
    if (step.screen != currentScreen) return

    val targetInfo = state.targets[step.id]
    val density = LocalDensity.current
    
    var hostWindowPos by remember { mutableStateOf(Offset.Zero) }
    var hostSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    hostWindowPos = coords.positionInWindow()
                    hostSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                }
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .clickable(enabled = true) {} // Intercept clicks
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw dimmed background
            drawRect(color = Color.Black.copy(alpha = 0.7f))
            
            if (hostSize != Size.Zero) {
                targetInfo?.let { (windowPos, size) ->
                    // Calculate position relative to the TourHost's coordinate system
                    val localPos = windowPos - hostWindowPos
                    
                    // Draw cutout
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = localPos.copy(x = localPos.x - 8.dp.toPx(), y = localPos.y - 8.dp.toPx()),
                        size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                    
                    // Highlight border
                    drawRoundRect(
                        color = Color(0xFF1DB954).copy(alpha = 0.5f),
                        topLeft = localPos.copy(x = localPos.x - 8.dp.toPx(), y = localPos.y - 8.dp.toPx()),
                        size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        if (hostSize != Size.Zero) {
            targetInfo?.let { (windowPos, size) ->
                val localPos = windowPos - hostWindowPos
                
                val spaceAbove = localPos.y
                val spaceBelow = hostSize.height - (localPos.y + size.height)
                val showAbove = spaceAbove > spaceBelow

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .then(
                                if (showAbove) {
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = with(density) { 
                                            (hostSize.height - localPos.y).coerceAtLeast(0f).toDp() + 16.dp 
                                        })
                                } else {
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = with(density) { 
                                            (localPos.y + size.height).coerceAtLeast(0f).toDp() + 16.dp 
                                        })
                                }
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        TourStepContent(step, state)
                    }
                }
            } ?: run {
                // fallback - show in center if target not found
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        TourStepContent(step, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun TourStepContent(step: TourStep, state: TourState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(step.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(step.description, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { state.skipTour() }) {
                Text("Skip", color = Color.Gray)
            }
            Button(
                onClick = { state.nextStep() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                val isLast = state.currentStepIndex == state.steps.size - 1
                Text(if (isLast) "Finish" else "Next", color = Color.Black)
            }
        }
    }
}
