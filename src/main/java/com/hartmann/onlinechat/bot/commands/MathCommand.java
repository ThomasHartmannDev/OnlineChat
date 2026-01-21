package com.hartmann.onlinechat.bot.commands;

import com.hartmann.onlinechat.bot.BotCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Bot command for performing mathematical operations.
 * Supports +, -, *, / with operator precedence (Point before Line).
 * 
 * @author Thomas Hartmann
 */
// START
@Component
@Slf4j
public class MathCommand implements BotCommand {

    @Override
    public String execute(String[] args, SimpMessageHeaderAccessor headerAccessor) {
        if (args.length == 0) {
            return "Usage: @server math <expression> (e.g., 2 + 3 * 4)";
        }

        // 1. Reconstruct the full expression string from arguments
        // (args might be split by spaces, but user could type "2+2")
        StringBuilder expressionBuilder = new StringBuilder();
        for (String arg : args) {
            expressionBuilder.append(arg);
        }
        String expression = expressionBuilder.toString();

        try {
            double result = evaluateExpression(expression);
            // Format result: if integer, show as integer
            if (result == (long) result) {
                return String.format("%d", (long) result);
            } else {
                return String.valueOf(result);
            }
        } catch (ArithmeticException e) {
            return "Error: " + e.getMessage(); // e.g., Division by zero
        } catch (Exception e) {
            log.error("Math parsing error: {}", expression, e);
            return "Error: Invalid expression";
        }
    }

    @Override
    public String getCommandName() {
        return "math";
    }

    /**
     * Evaluates a mathematical expression with operator precedence.
     * Supports +, -, *, /.
     */
    private double evaluateExpression(String expression) {
        // Remove spaces explicitly (spec says ignore spaces)
        expression = expression.replaceAll("\\s+", "");

        // Parse numbers and operators
        List<Double> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();

        StringBuilder currentNumber = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                currentNumber.append(c);
            } else if (isOperator(c)) {
                if (currentNumber.length() > 0) {
                    numbers.add(Double.parseDouble(currentNumber.toString()));
                    currentNumber.setLength(0);
                }
                operators.add(c);
            } else {
                throw new IllegalArgumentException("Invalid character: " + c);
            }
        }
        // Add the last number
        if (currentNumber.length() > 0) {
            numbers.add(Double.parseDouble(currentNumber.toString()));
        }

        if (numbers.size() == 0)
            return 0;
        if (numbers.size() != operators.size() + 1) {
            // Handle case like "-5" or trailing operator if necessary,
            // but simpler requirement implies "2 + 2".
            // Assume valid infix for now or throw.
            if (numbers.size() < operators.size() + 1)
                throw new IllegalArgumentException("Invalid format");
        }

        // 1. Handle Multiplication and Division first (Point)
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            if (op == '*' || op == '/') {
                double left = numbers.get(i);
                double right = numbers.get(i + 1);
                double res = 0;

                if (op == '*')
                    res = left * right;
                if (op == '/') {
                    if (right == 0)
                        throw new ArithmeticException("Division by zero");
                    res = left / right;
                }

                // Replace left/right with result
                numbers.set(i, res);
                numbers.remove(i + 1);
                operators.remove(i);
                i--; // Step back to check next operator at this index
            }
        }

        // 2. Handle Addition and Subtraction (Line)
        double result = numbers.get(0);
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            double nextVal = numbers.get(i + 1);

            if (op == '+')
                result += nextVal;
            if (op == '-')
                result -= nextVal;
        }

        return result;
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }
}
// END
