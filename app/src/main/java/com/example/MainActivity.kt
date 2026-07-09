package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SophisticatedBg)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    containerColor = SophisticatedBg
                ) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val haptic = LocalHapticFeedback.current

    // For portrait scientific drawer
    var isSciExpandedInPortrait by remember { mutableStateOf(false) }

    // Scroll state for displays
    val formulaScrollState = rememberScrollState()
    val displayScrollState = rememberScrollState()

    // Automatic scrolling to the end when inputs change
    LaunchedEffect(uiState.formula) {
        formulaScrollState.animateScrollTo(formulaScrollState.maxValue)
    }
    LaunchedEffect(uiState.display) {
        displayScrollState.animateScrollTo(displayScrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- SECTION 1: VISUAL DISPLAYS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            // Mode Banner (RAD or DEG, Memory Indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (uiState.isDegrees) SophisticatedOpBg.copy(alpha = 0.25f) else SophisticatedNumberBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (uiState.isDegrees) "DEG" else "RAD",
                            color = if (uiState.isDegrees) SophisticatedSciText else SophisticatedNumberText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (uiState.memory != 0.0) {
                        Surface(
                            color = SophisticatedUtilityBg.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "M",
                                color = SophisticatedNumberText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (uiState.isExpressionMode) {
                        Surface(
                            color = SophisticatedOpBg.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "EXPR",
                                color = SophisticatedSciText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Error message or orientation suggestion
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("error_label")
                    )
                } else if (!isLandscape) {
                    // Small rotate tip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable {
                            isSciExpandedInPortrait = !isSciExpandedInPortrait
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate for full Scientific board",
                            tint = SophisticatedFormulaText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Rotate for full board",
                            color = SophisticatedFormulaText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // A: Formula History Display (e.g. "sin(30) + 12 =")
            if (uiState.formula.isNotEmpty()) {
                Text(
                    text = uiState.formula,
                    color = SophisticatedFormulaText,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(formulaScrollState)
                        .testTag("formula_display_text")
                        .padding(vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            // B: Main Large Display (Interactive with Swipe-to-Delete)
            var swipeTriggered by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { swipeTriggered = false },
                            onDragEnd = { swipeTriggered = false },
                            onHorizontalDrag = { change, dragAmount ->
                                if (!swipeTriggered && abs(dragAmount) > 24) {
                                    viewModel.deleteLastDigit()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeTriggered = true
                                    change.consume()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                // Dynamic font size scaling to prevent wrapping or clipping on long strings
                val displayText = uiState.display
                val fontSize = when {
                    displayText.length <= 6 -> 74.sp
                    displayText.length <= 8 -> 60.sp
                    displayText.length <= 11 -> 46.sp
                    displayText.length <= 15 -> 34.sp
                    else -> 24.sp
                }

                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(displayScrollState)
                        .testTag("display_text")
                        .padding(vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }

        // --- SECTION 2: PORTRAIT CONTROLS ---
        if (!isLandscape) {
            // Mini utility bar with Sci Mode drawer toggle and RadDeg quick toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rad / Deg Toggle
                Button(
                    onClick = {
                        viewModel.onKeyPress(if (uiState.isDegrees) "Rad" else "Deg")
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SophisticatedNumberBg,
                        contentColor = SophisticatedNumberText
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_deg_rad_toggle")
                ) {
                    Text(
                        text = if (uiState.isDegrees) "Deg mode" else "Rad mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Expand Scientific Keypad drawer
                Button(
                    onClick = {
                        isSciExpandedInPortrait = !isSciExpandedInPortrait
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSciExpandedInPortrait) SophisticatedOpBg else SophisticatedNumberBg,
                        contentColor = if (isSciExpandedInPortrait) SophisticatedOpText else SophisticatedNumberText
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_sci_panel_toggle")
                ) {
                    Icon(
                        imageVector = if (isSciExpandedInPortrait) Icons.Default.KeyboardArrowDown else Icons.Default.Science,
                        contentDescription = "Toggle scientific keys",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSciExpandedInPortrait) "Basic Mode" else "Scientific",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Animated sliding scientific drawer in Portrait
            AnimatedVisibility(
                visible = isSciExpandedInPortrait,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                PortraitScientificDrawer(
                    viewModel = viewModel,
                    is2ndActive = uiState.is2ndActive,
                    haptic = haptic
                )
            }
        }

        // --- SECTION 3: KEYPADS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            if (isLandscape) {
                // Wide side-by-side scientific + standard layout
                LandscapeKeypad(
                    viewModel = viewModel,
                    uiState = uiState,
                    haptic = haptic
                )
            } else {
                // Classic standard basic vertical keypad
                PortraitKeypad(
                    viewModel = viewModel,
                    uiState = uiState,
                    haptic = haptic
                )
            }
        }
    }
}

@Composable
fun PortraitKeypad(
    viewModel: CalculatorViewModel,
    uiState: CalculatorViewModel.CalculatorUiState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    // Standard rows
    val rows = listOf(
        listOf(
            if (uiState.display == "0" && uiState.expression.isEmpty()) "AC" else "C",
            "+/-",
            "%",
            "÷"
        ),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (key in row) {
                    val isZero = key == "0"
                    val weight = if (isZero) 2f else 1f

                    val isEquals = key == "="
                    val isOperator = key in listOf("÷", "×", "-", "+")
                    val isHelper = key in listOf("AC", "C", "+/-", "%")

                    // Track active state of operators
                    val isOperatorActive = uiState.activeOperator == key && !uiState.isResultShown

                    val containerColor = when {
                        isOperatorActive -> SophisticatedOpText
                        isOperator -> SophisticatedOpBg
                        isEquals -> SophisticatedEqualsBg
                        isHelper -> SophisticatedUtilityBg
                        else -> SophisticatedNumberBg
                    }

                    val textColor = when {
                        isOperatorActive -> SophisticatedOpBg
                        isOperator -> SophisticatedOpText
                        isEquals -> SophisticatedEqualsText
                        isHelper -> SophisticatedUtilityText
                        else -> SophisticatedNumberText
                    }

                    val pressedColor = when {
                        isOperator -> SophisticatedEqualsBg
                        isEquals -> Color.White
                        isHelper -> Color.White
                        else -> iOSGrayPressed
                    }

                    CalculatorButton(
                        text = key,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onKeyPress(key)
                        },
                        containerColor = containerColor,
                        textColor = textColor,
                        pressedColor = pressedColor,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(weight)
                            .aspectRatio(if (isZero) 2.2f else 1f)
                            .testTag("btn_${key.lowercase()}")
                    )

                }
            }
        }
    }
}

@Composable
fun LandscapeKeypad(
    viewModel: CalculatorViewModel,
    uiState: CalculatorViewModel.CalculatorUiState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    // 5 Rows of scientific keys (Left side: 6 columns) + basic keys (Right side: 4 columns)
    // Combined row list
    val keyRows = listOf(
        // Row 1
        listOf("(", ")", "mc", "m+", "m-", "mr") to listOf(if (uiState.display == "0" && uiState.expression.isEmpty()) "AC" else "C", "+/-", "%", "÷"),
        // Row 2
        listOf("2nd", "x²", "x³", "x^y", "e^x", "10^x") to listOf("7", "8", "9", "×"),
        // Row 3
        listOf("1/x", "√x", "³√x", "y^√x", "ln", "log₁₀") to listOf("4", "5", "6", "-"),
        // Row 4
        listOf("x!", "sin", "cos", "tan", "e", "EE") to listOf("1", "2", "3", "+"),
        // Row 5
        listOf("Rad", "sinh", "cosh", "tanh", "π", "Rand") to listOf("0", ".", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((sciRow, stdRow) in keyRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Column: Scientific board (6 buttons wide)
                Row(
                    modifier = Modifier.weight(1.5f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (key in sciRow) {
                        // Support secondary functions (arcsin, arccos, arctan)
                        val displayKey = when {
                            uiState.is2ndActive && key == "sin" -> "sin⁻¹"
                            uiState.is2ndActive && key == "cos" -> "cos⁻¹"
                            uiState.is2ndActive && key == "tan" -> "tan⁻¹"
                            else -> key
                        }

                        // Determine colors
                        val is2ndToggleActive = key == "2nd" && uiState.is2ndActive
                        val containerColor = if (is2ndToggleActive) SophisticatedOpText else SophisticatedSciBg
                        val textColor = if (is2ndToggleActive) SophisticatedOpBg else SophisticatedSciText

                        CalculatorButton(
                            text = displayKey,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onKeyPress(key)
                            },
                            containerColor = containerColor,
                            textColor = textColor,
                            pressedColor = iOSGrayPressed,
                            fontSize = 12.sp,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SophisticatedSciBorder),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .testTag("btn_sci_${key.lowercase()}")
                        )
                    }
                }

                // Right Column: Standard board (4 buttons wide)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (key in stdRow) {
                        val isZero = key == "0"
                        val weight = if (isZero) 2f else 1f

                        val isEquals = key == "="
                        val isOperator = key in listOf("÷", "×", "-", "+")
                        val isHelper = key in listOf("AC", "C", "+/-", "%")
                        val isOperatorActive = uiState.activeOperator == key && !uiState.isResultShown

                        val containerColor = when {
                            isOperatorActive -> SophisticatedOpText
                            isOperator -> SophisticatedOpBg
                            isEquals -> SophisticatedEqualsBg
                            isHelper -> SophisticatedUtilityBg
                            else -> SophisticatedNumberBg
                        }

                        val textColor = when {
                            isOperatorActive -> SophisticatedOpBg
                            isOperator -> SophisticatedOpText
                            isEquals -> SophisticatedEqualsText
                            isHelper -> SophisticatedUtilityText
                            else -> SophisticatedNumberText
                        }

                        val pressedColor = when {
                            isOperator -> SophisticatedEqualsBg
                            isEquals -> Color.White
                            isHelper -> Color.White
                            else -> iOSGrayPressed
                        }

                        CalculatorButton(
                            text = key,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onKeyPress(key)
                            },
                            containerColor = containerColor,
                            textColor = textColor,
                            pressedColor = pressedColor,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(weight)
                                .aspectRatio(if (isZero) 1.9f else 1.4f)
                                .testTag("btn_${key.lowercase()}")
                        )
                    }
                }

            }
        }
    }
}

// 12 Essential Scientific Keys for Portrait Screen
@Composable
fun PortraitScientificDrawer(
    viewModel: CalculatorViewModel,
    is2ndActive: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val keys = listOf(
        listOf("(", ")", "x²", "x^y"),
        listOf("sin", "cos", "tan", "ln"),
        listOf("log₁₀", "x!", "e", "π")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SophisticatedNumberBg, shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, SophisticatedSciBorder), shape = RoundedCornerShape(16.dp))
            .padding(10.dp)
            .testTag("portrait_scientific_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scientific Pad",
                color = SophisticatedFormulaText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Secondary toggle key inside portrait drawer
            Surface(
                color = if (is2ndActive) SophisticatedOpBg else SophisticatedSciBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SophisticatedSciBorder),
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onKeyPress("2nd")
                    }
                    .testTag("btn_sci_2nd_toggle")
            ) {
                Text(
                    text = "2nd Function",
                    color = if (is2ndActive) SophisticatedOpText else SophisticatedNumberText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (key in row) {
                    val displayKey = when {
                        is2ndActive && key == "sin" -> "sin⁻¹"
                        is2ndActive && key == "cos" -> "cos⁻¹"
                        is2ndActive && key == "tan" -> "tan⁻¹"
                        else -> key
                    }

                    CalculatorButton(
                        text = displayKey,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onKeyPress(key)
                        },
                        containerColor = SophisticatedSciBg,
                        textColor = SophisticatedSciText,
                        pressedColor = iOSGrayPressed,
                        fontSize = 14.sp,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SophisticatedSciBorder),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.8f)
                            .testTag("btn_sci_${key.lowercase()}")
                    )
                }
            }
        }
    }
}


// Highly stylized circular/pill shaped calculator button
@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    textColor: Color,
    pressedColor: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    border: androidx.compose.foundation.BorderStroke? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentBg = if (isPressed) pressedColor else containerColor

    var boxModifier = modifier
        .clip(shape)
    if (border != null) {
        boxModifier = boxModifier.border(border, shape)
    }

    Box(
        modifier = boxModifier
            .background(currentBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple so custom haptic is immediate
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center
        )
    }
}

