package com.example

import kotlin.math.*

object MathEvaluator {
    
    class ParseException(message: String) : Exception(message)

    fun evaluate(expression: String, isDegrees: Boolean = false): Double {
        val cleaned = preprocess(expression)
        val parser = Parser(cleaned, isDegrees)
        return parser.parse()
    }

    private fun preprocess(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace("EE", "*10^")
    }

    private class Parser(val input: String, val isDegrees: Boolean) {
        var pos = 0

        fun parse(): Double {
            if (input.trim().isEmpty()) return 0.0
            val result = parseExpression()
            if (pos < input.length) {
                throw ParseException("Unexpected character: " + input[pos])
            }
            return result
        }

        private fun peek(): Char {
            return if (pos < input.length) input[pos] else '\u0000'
        }

        private fun consume(): Char {
            val c = peek()
            if (c != '\u0000') pos++
            return c
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        // Expression = Term [ ('+' | '-') Term ]*
        private fun parseExpression(): Double {
            skipWhitespace()
            var result = parseTerm()
            while (true) {
                skipWhitespace()
                val c = peek()
                if (c == '+') {
                    consume()
                    result += parseTerm()
                } else if (c == '-') {
                    consume()
                    result -= parseTerm()
                } else {
                    break
                }
            }
            return result
        }

        // Term = Factor [ ('*' | '/' | '%') Factor ]*
        private fun parseTerm(): Double {
            skipWhitespace()
            var result = parseFactor()
            while (true) {
                skipWhitespace()
                val c = peek()
                if (c == '*') {
                    consume()
                    result *= parseFactor()
                } else if (c == '/') {
                    consume()
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ParseException("Division by zero")
                    result /= divisor
                } else if (c == '%') {
                    consume()
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ParseException("Division by zero")
                    result %= divisor
                } else {
                    // Check for implicit multiplication, e.g., 2pi or 5(3+2) or 5sin(30)
                    skipWhitespace()
                    val next = peek()
                    if (next == '(' || next == 'p' || next == 'e' || next.isLetter()) {
                        result *= parseFactor()
                    } else {
                        break
                    }
                }
            }
            return result
        }

        // Factor = Power [ '^' Power ]*
        private fun parseFactor(): Double {
            skipWhitespace()
            var result = parsePower()
            while (true) {
                skipWhitespace()
                if (peek() == '^') {
                    consume()
                    result = result.pow(parsePower())
                } else {
                    break
                }
            }
            return result
        }

        // Power = [ '+' | '-' ]? Primary [ '!' ]*
        private fun parsePower(): Double {
            skipWhitespace()
            var sign = 1.0
            if (peek() == '+') {
                consume()
            } else if (peek() == '-') {
                consume()
                sign = -1.0
            }

            var result = parsePrimary() * sign

            // Check for factorial
            while (true) {
                skipWhitespace()
                if (peek() == '!') {
                    consume()
                    result = factorial(result)
                } else {
                    break
                }
            }
            return result
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            val c = peek()

            if (c == '(') {
                consume()
                val result = parseExpression()
                skipWhitespace()
                if (consume() != ')') {
                    throw ParseException("Expected ')'")
                }
                return result
            }

            if (c == 'e') {
                consume()
                return E
            }

            if (c == 'p' && pos + 1 < input.length && input[pos + 1] == 'i') {
                consume() // p
                consume() // i
                return PI
            }

            if (c.isDigit() || c == '.') {
                return parseNumber()
            }

            if (c.isLetter()) {
                return parseFunction()
            }

            throw ParseException("Unexpected character: $c")
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < input.length) {
                val c = input[pos]
                if (c.isDigit() || c == '.' || c == 'E' || c == 'e') {
                    // Handle scientific notation inside numbers, like 1e5 or 2.3E-4
                    if ((c == 'e' || c == 'E') && pos + 1 < input.length) {
                        val next = input[pos + 1]
                        if (next == '+' || next == '-' || next.isDigit()) {
                            pos += 2
                            while (pos < input.length && input[pos].isDigit()) {
                                pos++
                            }
                            continue
                        }
                    }
                    pos++
                } else {
                    break
                }
            }
            val numStr = input.substring(start, pos)
            try {
                return numStr.toDouble()
            } catch (e: NumberFormatException) {
                throw ParseException("Invalid number: $numStr")
            }
        }

        private fun parseFunction(): Double {
            val start = pos
            while (pos < input.length && input[pos].isLetter()) {
                pos++
            }
            val funcName = input.substring(start, pos)
            skipWhitespace()
            if (consume() != '(') {
                throw ParseException("Expected '(' after function name $funcName")
            }
            val arg = parseExpression()
            skipWhitespace()
            if (consume() != ')') {
                throw ParseException("Expected ')' after function argument")
            }

            return when (funcName) {
                "sin" -> if (isDegrees) sin(Math.toRadians(arg)) else sin(arg)
                "cos" -> if (isDegrees) cos(Math.toRadians(arg)) else cos(arg)
                "tan" -> if (isDegrees) tan(Math.toRadians(arg)) else tan(arg)
                "asin" -> if (isDegrees) Math.toDegrees(asin(arg)) else asin(arg)
                "acos" -> if (isDegrees) Math.toDegrees(acos(arg)) else acos(arg)
                "atan" -> if (isDegrees) Math.toDegrees(atan(arg)) else atan(arg)
                "sinh" -> sinh(arg)
                "cosh" -> cosh(arg)
                "tanh" -> tanh(arg)
                "ln" -> {
                    if (arg <= 0) throw ParseException("Domain error for ln")
                    ln(arg)
                }
                "log" -> {
                    if (arg <= 0) throw ParseException("Domain error for log")
                    log10(arg)
                }
                "sqrt" -> {
                    if (arg < 0) throw ParseException("Square root of negative number")
                    sqrt(arg)
                }
                "cbrt" -> {
                    cbrt(arg)
                }
                "exp" -> exp(arg)
                else -> throw ParseException("Unknown function: $funcName")
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n % 1.0 != 0.0) {
                throw ParseException("Factorial only defined for non-negative integers")
            }
            val intN = n.toInt()
            if (intN > 170) return Double.POSITIVE_INFINITY
            var result = 1.0
            for (i in 1..intN) {
                result *= i
            }
            return result
        }
    }
}
