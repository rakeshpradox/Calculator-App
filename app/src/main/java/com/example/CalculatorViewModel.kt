package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlin.math.*

class CalculatorViewModel : ViewModel() {

    // UI state holder
    data class CalculatorUiState(
        val display: String = "0",            // Large main display text
        val expression: String = "",         // Running math expression
        val formula: String = "",            // Elegant visual expression shown above (e.g. sin(30°) + 10)
        val isDegrees: Boolean = true,       // Deg or Rad mode
        val memory: Double = 0.0,            // Memory register
        val is2ndActive: Boolean = false,     // "2nd" function key toggle state
        val activeOperator: String? = null,  // Currently active standard operator (+, -, ×, ÷)
        val isExpressionMode: Boolean = false, // If true, we are typing a scientific expression. Otherwise, basic instant mode
        val error: String? = null,           // Displays evaluation errors
        val isResultShown: Boolean = false   // True if the current display is a calculated result
    )

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    // Helper state for basic mode instant calculation
    private var prevOperand: Double? = null
    private var lastClickedOperator: String? = null
    private var lastOperand: Double? = null // For repeating operation on consecutive '=' presses

    fun onKeyPress(key: String) {
        val current = _uiState.value
        _uiState.value = current.copy(error = null) // Reset error

        when (key) {
            "AC" -> clearAll()
            "C" -> clearDisplay()
            "+/-" -> toggleSign()
            "%" -> applyPercent()
            "+" -> handleOperator("+")
            "-" -> handleOperator("-")
            "×" -> handleOperator("×")
            "÷" -> handleOperator("÷")
            "=" -> evaluateResult()
            "." -> appendDecimal()
            "2nd" -> toggle2nd()
            "Rad", "Deg" -> toggleRadDeg()
            "mc" -> clearMemory()
            "mr" -> recallMemory()
            "m+" -> addToMemory()
            "m-" -> subtractFromMemory()
            "Rand" -> insertRandom()
            "x²" -> applyPowerOf2()
            "x³" -> applyPowerOf3()
            "x^y" -> handlePowerOfY()
            "e^x" -> appendScientificFunction("exp(")
            "10^x" -> appendScientificFunction("10^(")
            "1/x" -> applyReciprocal()
            "√x" -> appendScientificFunction("sqrt(")
            "³√x" -> appendScientificFunction("cbrt(")
            "ln" -> appendScientificFunction("ln(")
            "log₁₀" -> appendScientificFunction("log(")
            "sin" -> appendScientificFunction(if (current.is2ndActive) "asin(" else "sin(")
            "cos" -> appendScientificFunction(if (current.is2ndActive) "acos(" else "cos(")
            "tan" -> appendScientificFunction(if (current.is2ndActive) "atan(" else "tan(")
            "sinh" -> appendScientificFunction("sinh(")
            "cosh" -> appendScientificFunction("cosh(")
            "tanh" -> appendScientificFunction("tanh(")
            "x!" -> appendFactorial()
            "e" -> appendConstant("e")
            "π" -> appendConstant("π")
            "(" -> appendParenthesis("(")
            ")" -> appendParenthesis(")")
            "EE" -> appendScientificOperator("EE")
            else -> {
                // If it's a digit 0-9
                if (key.all { it.isDigit() }) {
                    appendDigit(key)
                }
            }
        }
    }

    private fun clearAll() {
        prevOperand = null
        lastClickedOperator = null
        lastOperand = null
        _uiState.value = CalculatorUiState(isDegrees = _uiState.value.isDegrees, memory = _uiState.value.memory)
    }

    private fun clearDisplay() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            // In expression mode, C clears the entire current expression
            _uiState.value = current.copy(
                expression = "",
                formula = "",
                isExpressionMode = false,
                display = "0"
            )
        } else {
            // In basic mode, C clears only the active display text
            _uiState.value = current.copy(display = "0")
        }
    }

    private fun toggleSign() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            // If in expression mode, prefix the whole expression or wrap in negate
            if (current.expression.startsWith("-(")) {
                // Remove negating parentheses
                val unwrapped = current.expression.removePrefix("-(").removeSuffix(")")
                _uiState.value = current.copy(
                    expression = unwrapped,
                    formula = unwrapped,
                    display = unwrapped
                )
            } else {
                _uiState.value = current.copy(
                    expression = "-(${current.expression})",
                    formula = "-(${current.formula})",
                    display = "-(${current.expression})"
                )
            }
        } else {
            val d = current.display.toDoubleOrNull()
            if (d != null && d != 0.0) {
                _uiState.value = current.copy(display = formatResult(-d))
            }
        }
    }

    private fun applyPercent() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + "%",
                formula = current.formula + "%",
                display = current.expression + "%"
            )
        } else {
            val d = current.display.toDoubleOrNull()
            if (d != null) {
                _uiState.value = current.copy(display = formatResult(d / 100.0))
            }
        }
    }

    private fun appendDigit(digit: String) {
        val current = _uiState.value

        if (current.isExpressionMode) {
            val newExpr = current.expression + digit
            val newFormula = current.formula + digit
            _uiState.value = current.copy(
                expression = newExpr,
                formula = newFormula,
                display = newExpr
            )
        } else {
            // Basic instant calculation mode
            val isFreshInput = current.isResultShown || current.activeOperator != null && current.display == formatResult(prevOperand ?: 0.0)
            
            val newDisplay = if (current.display == "0" || isFreshInput) {
                digit
            } else {
                if (current.display.length >= 15) return // Limit input length
                current.display + digit
            }

            _uiState.value = current.copy(
                display = newDisplay,
                isResultShown = false
            )
        }
    }

    private fun appendDecimal() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            if (current.expression.isNotEmpty() && current.expression.last().isDigit()) {
                val lastNum = current.expression.split("[+\\-*×/÷()^! ]".toRegex()).last()
                if (!lastNum.contains(".")) {
                    _uiState.value = current.copy(
                        expression = current.expression + ".",
                        formula = current.formula + ".",
                        display = current.expression + "."
                    )
                }
            } else {
                _uiState.value = current.copy(
                    expression = current.expression + "0.",
                    formula = current.formula + "0.",
                    display = current.expression + "0."
                )
            }
        } else {
            val isFreshInput = current.isResultShown || current.activeOperator != null && current.display == formatResult(prevOperand ?: 0.0)
            if (isFreshInput) {
                _uiState.value = current.copy(display = "0.", isResultShown = false)
            } else if (!current.display.contains(".")) {
                _uiState.value = current.copy(display = current.display + ".")
            }
        }
    }

    private fun handleOperator(op: String) {
        val current = _uiState.value

        if (current.isExpressionMode) {
            // In scientific expression mode, operators append directly to the equation
            val displayOp = when (op) {
                "×" -> "×"
                "÷" -> "÷"
                else -> op
            }
            _uiState.value = current.copy(
                expression = current.expression + " " + op + " ",
                formula = current.formula + " " + displayOp + " ",
                display = current.expression + " " + op + " "
            )
        } else {
            // Basic instant calculator mode
            val currentVal = current.display.toDoubleOrNull() ?: 0.0
            
            if (prevOperand != null && lastClickedOperator != null && !current.isResultShown) {
                // Instant sequential calculation e.g. 5 + 3 - -> calculates 8, sets display, queue next
                try {
                    val result = executeBasicOperation(prevOperand!!, lastClickedOperator!!, currentVal)
                    prevOperand = result
                    _uiState.value = current.copy(
                        display = formatResult(result),
                        activeOperator = op,
                        isResultShown = false
                    )
                } catch (e: Exception) {
                    _uiState.value = current.copy(error = e.message ?: "Error", activeOperator = null)
                    prevOperand = null
                }
            } else {
                prevOperand = currentVal
                _uiState.value = current.copy(
                    activeOperator = op,
                    isResultShown = false
                )
            }
            lastClickedOperator = op
        }
    }

    private fun executeBasicOperation(op1: Double, operator: String, op2: Double): Double {
        return when (operator) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "×", "*" -> op1 * op2
            "÷", "/" -> {
                if (op2 == 0.0) throw ArithmeticException("Error: Div by 0")
                op1 / op2
            }
            else -> op2
        }
    }

    private fun evaluateResult() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            // Evaluate complex scientific math expression
            try {
                if (current.expression.trim().isEmpty()) return
                val result = MathEvaluator.evaluate(current.expression, current.isDegrees)
                val formatted = formatResult(result)
                _uiState.value = current.copy(
                    display = formatted,
                    isResultShown = true,
                    // Store evaluated expression in the formula field as a nice record
                    formula = current.formula + " =",
                    isExpressionMode = false
                )
            } catch (e: Exception) {
                _uiState.value = current.copy(error = e.message ?: "Syntax Error", display = "Error")
            }
        } else {
            // Basic mode evaluation
            val currentVal = current.display.toDoubleOrNull() ?: 0.0
            if (prevOperand != null && lastClickedOperator != null) {
                try {
                    lastOperand = currentVal
                    val result = executeBasicOperation(prevOperand!!, lastClickedOperator!!, currentVal)
                    _uiState.value = current.copy(
                        display = formatResult(result),
                        activeOperator = null,
                        isResultShown = true
                    )
                    prevOperand = null
                } catch (e: Exception) {
                    _uiState.value = current.copy(error = e.message ?: "Error", display = "Error")
                }
            } else if (lastOperand != null && lastClickedOperator != null) {
                // Repeat operation on consecutive '=' presses (e.g. 5 + 3 = 8, = 11, = 14)
                try {
                    val result = executeBasicOperation(currentVal, lastClickedOperator!!, lastOperand!!)
                    _uiState.value = current.copy(
                        display = formatResult(result),
                        isResultShown = true
                    )
                } catch (e: Exception) {
                    _uiState.value = current.copy(error = e.message ?: "Error", display = "Error")
                }
            }
        }
    }

    // Helper to format Double back to visual string cleanly
    fun formatResult(value: Double): String {
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        if (value.isNaN()) return "Error"

        // Handle very large or very small scientific numbers
        if (abs(value) >= 1e11 || (abs(value) < 1e-4 && value != 0.0)) {
            return String.format("%.6e", value).replace("e+", "e").replace("e0", "e").replace(".000000", "")
        }

        val formatted = String.format("%.10f", value)
        // Trim trailing zeros and decimal point if not needed
        var clean = formatted
        if (clean.contains(".")) {
            clean = clean.trimEnd('0')
            if (clean.endsWith(".")) {
                clean = clean.substring(0, clean.length - 1)
            }
        }
        return clean
    }

    // --- Scientific Actions ---

    private fun toggle2nd() {
        val current = _uiState.value
        _uiState.value = current.copy(is2ndActive = !current.is2ndActive)
    }

    private fun toggleRadDeg() {
        val current = _uiState.value
        _uiState.value = current.copy(isDegrees = !current.isDegrees)
    }

    private fun clearMemory() {
        _uiState.value = _uiState.value.copy(memory = 0.0)
    }

    private fun recallMemory() {
        val current = _uiState.value
        val memValueStr = formatResult(current.memory)
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + memValueStr,
                formula = current.formula + memValueStr,
                display = current.expression + memValueStr
            )
        } else {
            _uiState.value = current.copy(display = memValueStr, isResultShown = true)
        }
    }

    private fun addToMemory() {
        val current = _uiState.value
        val d = current.display.toDoubleOrNull() ?: 0.0
        _uiState.value = current.copy(memory = current.memory + d)
    }

    private fun subtractFromMemory() {
        val current = _uiState.value
        val d = current.display.toDoubleOrNull() ?: 0.0
        _uiState.value = current.copy(memory = current.memory - d)
    }

    private fun insertRandom() {
        val current = _uiState.value
        val rand = Random.nextDouble()
        val randStr = formatResult(rand)
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + randStr,
                formula = current.formula + randStr,
                display = current.expression + randStr
            )
        } else {
            _uiState.value = current.copy(display = randStr, isResultShown = true)
        }
    }

    private fun applyPowerOf2() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + "^2",
                formula = current.formula + "²",
                display = current.expression + "^2"
            )
        } else {
            val d = current.display.toDoubleOrNull()
            if (d != null) {
                _uiState.value = current.copy(display = formatResult(d * d), isResultShown = true)
            }
        }
    }

    private fun applyPowerOf3() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + "^3",
                formula = current.formula + "³",
                display = current.expression + "^3"
            )
        } else {
            val d = current.display.toDoubleOrNull()
            if (d != null) {
                _uiState.value = current.copy(display = formatResult(d * d * d), isResultShown = true)
            }
        }
    }

    private fun handlePowerOfY() {
        val current = _uiState.value
        // Automatically switches to Expression Mode because ^y is an expression operator
        val baseExpr = if (current.isExpressionMode) current.expression else current.display
        val baseFormula = if (current.isExpressionMode) current.formula else current.display
        _uiState.value = current.copy(
            isExpressionMode = true,
            expression = "$baseExpr^",
            formula = "$baseFormula^",
            display = "$baseExpr^"
        )
    }

    private fun applyReciprocal() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + "1/(",
                formula = current.formula + "1/(",
                display = current.expression + "1/("
            )
        } else {
            val d = current.display.toDoubleOrNull()
            if (d != null && d != 0.0) {
                _uiState.value = current.copy(display = formatResult(1.0 / d), isResultShown = true)
            } else if (d == 0.0) {
                _uiState.value = current.copy(error = "Division by zero", display = "Error")
            }
        }
    }

    private fun appendScientificFunction(func: String) {
        val current = _uiState.value
        
        // Pretty labels for formula bar
        val displayFunc = when (func) {
            "exp(" -> "e^("
            "sqrt(" -> "√("
            "cbrt(" -> "³√("
            "asin(" -> "sin⁻¹("
            "acos(" -> "cos⁻¹("
            "atan(" -> "tan⁻¹("
            else -> func
        }

        val baseExpr = if (current.isExpressionMode) current.expression else ""
        val baseFormula = if (current.isExpressionMode) current.formula else ""

        _uiState.value = current.copy(
            isExpressionMode = true,
            expression = baseExpr + func,
            formula = baseFormula + displayFunc,
            display = baseExpr + func
        )
    }

    private fun appendFactorial() {
        val current = _uiState.value
        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + "!",
                formula = current.formula + "!",
                display = current.expression + "!"
            )
        } else {
            // Instant factorial
            val d = current.display.toDoubleOrNull()
            if (d != null) {
                try {
                    val result = evaluateFactorialDirectly(d)
                    _uiState.value = current.copy(display = formatResult(result), isResultShown = true)
                } catch (e: Exception) {
                    _uiState.value = current.copy(error = e.message ?: "Error", display = "Error")
                }
            }
        }
    }

    private fun evaluateFactorialDirectly(n: Double): Double {
        if (n < 0 || n % 1.0 != 0.0) {
            throw ArithmeticException("Error: Factorial non-integer")
        }
        val intN = n.toInt()
        if (intN > 170) return Double.POSITIVE_INFINITY
        var result = 1.0
        for (i in 1..intN) {
            result *= i
        }
        return result
    }

    private fun appendConstant(const: String) {
        val current = _uiState.value
        val constVal = if (const == "e") "e" else "π"

        if (current.isExpressionMode) {
            _uiState.value = current.copy(
                expression = current.expression + constVal,
                formula = current.formula + constVal,
                display = current.expression + constVal
            )
        } else {
            // Set directly
            val valStr = if (const == "e") formatResult(Math.E) else formatResult(Math.PI)
            _uiState.value = current.copy(
                display = valStr,
                isResultShown = true
            )
        }
    }

    private fun appendParenthesis(paren: String) {
        val current = _uiState.value
        val baseExpr = if (current.isExpressionMode) current.expression else ""
        val baseFormula = if (current.isExpressionMode) current.formula else ""

        _uiState.value = current.copy(
            isExpressionMode = true,
            expression = baseExpr + paren,
            formula = baseFormula + paren,
            display = baseExpr + paren
        )
    }

    private fun appendScientificOperator(op: String) {
        val current = _uiState.value
        val baseExpr = if (current.isExpressionMode) current.expression else current.display
        val baseFormula = if (current.isExpressionMode) current.formula else current.display

        _uiState.value = current.copy(
            isExpressionMode = true,
            expression = baseExpr + op,
            formula = baseFormula + "e", // EE is represented as 'e' in scientific notations visually
            display = baseExpr + op
        )
    }

    // Swipe left/right gesture deletes the last character typed
    fun deleteLastDigit() {
        val current = _uiState.value
        if (current.isResultShown) return // Don't delete on result

        if (current.isExpressionMode) {
            if (current.expression.isNotEmpty()) {
                var newExpr = current.expression.trimEnd()
                var newFormula = current.formula.trimEnd()

                // If it ends with a function word like "sin(", "cos(", "ln(", delete the whole token
                val funcSuffixes = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "cbrt(", "log(", "exp(")
                var deletedFunc = false
                for (suffix in funcSuffixes) {
                    if (newExpr.endsWith(suffix)) {
                        newExpr = newExpr.removeSuffix(suffix)
                        // Also adjust formula suffix
                        val displaySuffix = when(suffix) {
                            "exp(" -> "e^("
                            "sqrt(" -> "√("
                            "cbrt(" -> "³√("
                            "asin(" -> "sin⁻¹("
                            "acos(" -> "cos⁻¹("
                            "atan(" -> "tan⁻¹("
                            else -> suffix
                        }
                        newFormula = newFormula.removeSuffix(displaySuffix)
                        deletedFunc = true
                        break
                    }
                }

                if (!deletedFunc) {
                    newExpr = newExpr.dropLast(1)
                    newFormula = newFormula.dropLast(1)
                }

                _uiState.value = current.copy(
                    expression = newExpr,
                    formula = newFormula,
                    display = if (newExpr.isEmpty()) "0" else newExpr,
                    isExpressionMode = newExpr.isNotEmpty()
                )
            }
        } else {
            // Delete standard display number
            if (current.display != "0" && current.display.isNotEmpty()) {
                val newDisplay = current.display.dropLast(1)
                _uiState.value = current.copy(
                    display = if (newDisplay.isEmpty() || newDisplay == "-") "0" else newDisplay
                )
            }
        }
    }
}
