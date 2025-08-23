package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calculator plugin that performs basic mathematical operations.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class CalculatorPlugin implements AgentPlugin {

    private Map<String, Object> configuration;
    private static final Pattern CALCULATION_PATTERN = 
        Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(?:\\.\\d+)?)");

    @Override
    public void initialize(Map<String, Object> configuration) throws PluginException {
        this.configuration = configuration;
    }

    @Override
    public PluginResult execute(PluginContext context) throws PluginException {
        long startTime = System.currentTimeMillis();
        
        try {
            String expression = context.getParameter("expression", String.class);
            if (expression == null) {
                expression = context.getCommand();
            }
            
            // Extract calculation from the command
            Matcher matcher = CALCULATION_PATTERN.matcher(expression);
            
            if (!matcher.find()) {
                return PluginResult.failure("No valid calculation found in: " + expression);
            }
            
            double operand1 = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double operand2 = Double.parseDouble(matcher.group(3));
            
            double result = performCalculation(operand1, operator, operand2);
            String calculation = operand1 + " " + operator + " " + operand2 + " = " + result;
            
            return PluginResult.builder()
                .message("Calculation result: " + calculation)
                .data("expression", operand1 + " " + operator + " " + operand2)
                .data("result", result)
                .data("formatted", calculation)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
                
        } catch (NumberFormatException e) {
            throw new PluginException(getName(), 
                PluginException.PluginErrorCode.EXECUTION_FAILED,
                "Invalid number format in calculation", e);
        } catch (ArithmeticException e) {
            throw new PluginException(getName(), 
                PluginException.PluginErrorCode.EXECUTION_FAILED,
                "Arithmetic error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PluginException(getName(), 
                PluginException.PluginErrorCode.EXECUTION_FAILED,
                "Calculator plugin execution failed", e);
        }
    }

    private double performCalculation(double operand1, String operator, double operand2) 
            throws ArithmeticException {
        return switch (operator) {
            case "+" -> operand1 + operand2;
            case "-" -> operand1 - operand2;
            case "*" -> operand1 * operand2;
            case "/" -> {
                if (operand2 == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield operand1 / operand2;
            }
            default -> throw new ArithmeticException("Unsupported operator: " + operator);
        };
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    @Override
    public String getName() {
        return "calculator-plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginType getType() {
        return PluginType.COMMAND_PROCESSOR;
    }

    @Override
    public String getDescription() {
        return "Plugin that performs basic mathematical calculations (+, -, *, /)";
    }

    @Override
    public boolean supports(String command) {
        return command != null && 
               (command.toLowerCase().contains("calculate") || 
                command.toLowerCase().contains("math") ||
                CALCULATION_PATTERN.matcher(command).find());
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        return true; // No special configuration required
    }
}