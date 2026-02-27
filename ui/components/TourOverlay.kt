package com.example.payman.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
    state.targets[stepId] = coords.positionInRoot() to Size(coords.size.width.toFloat(), coords.size.height.toFloat())
}

@Composable
fun TourHost(state: TourState, currentScreen: String) {
    if (!state.isVisible || state.currentStep == null) return
    
    val step = state.currentStep!!
    if (step.screen != currentScreen) return

    val targetInfo = state.targets[step.id]
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .clickable(enabled = false) {} // Intercept clicks
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            
            // Draw dimmed background
            drawRect(color = Color.Black.copy(alpha = 0.7f))
            
            targetInfo?.let { (position, size) ->
                // Draw cutout
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = position.copy(x = position.x - 8.dp.toPx(), y = position.y - 8.dp.toPx()),
                    size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        targetInfo?.let { (position, size) ->
            val tooltipY = if (position.y > 400.dp.value) {
                position.y - 200.dp.value // Show above
            } else {
                position.y + size.height + 16.dp.value // Show below
            }

            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .offset(y = with(density) { tooltipY.toDp() })
                    .fillMaxWidth(0.8f)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
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
                            Text(if (state.currentStepIndex == state.steps.size - 1) "Finish" else "Next", color = Color.Black)
                        }
                    }
                }
            }
        } ?: run {
            // If target not found (e.g. scrolled off), show centered tooltip
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.8f)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                shape = RoundedCornerShape(16.dp)
            ) {
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
                            Text("Next", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
