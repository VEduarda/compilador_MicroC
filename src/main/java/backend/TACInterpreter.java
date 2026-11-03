package backend;

import optimizer.TACInstruction;
import optimizer.TACInstruction.Kind;

import java.util.*;

/**
 * Interpretador de Código Intermediário de Três Endereços (TAC) - Back-End do MicroC.
 *
 * Implementa a máquina virtual de execução do código intermediário (Item 4 do Enunciado).
 * Permite executar tanto o código TAC original quanto o código TAC otimizado,
 * demonstrando que a otimização preserva estritamente o comportamento observável.
 */
public class TACInterpreter {

    private boolean printToConsole = true;
    private int maxCycles = 1_000_000;

    public TACInterpreter() {
        this(true);
    }

    public TACInterpreter(boolean printToConsole) {
        this.printToConsole = printToConsole;
    }

    public void setPrintToConsole(boolean printToConsole) {
        this.printToConsole = printToConsole;
    }

    public void setMaxCycles(int maxCycles) {
        this.maxCycles = maxCycles;
    }

    /**
     * Executa uma sequência de instruções TAC lineares e retorna o resultado da execução.
     */
    public ExecutionResult execute(List<String> tacInstructions) {
        ExecutionResult result = new ExecutionResult();
        if (tacInstructions == null || tacInstructions.isEmpty()) {
            return result;
        }

        // 1. Faz o parsing das instruções para representação estruturada
        List<TACInstruction> program = new ArrayList<>();
        Map<String, Integer> labelMap = new HashMap<>();

        for (String line : tacInstructions) {
            TACInstruction inst = TACInstruction.parse(line);
            if (inst != null && inst.kind != Kind.COMMENT) {
                int index = program.size();
                program.add(inst);
                if (inst.kind == Kind.LABEL) {
                    labelMap.put(inst.label, index);
                }
            }
        }

        // 2. Estado de execução: memória de variáveis e ponteiro de instrução (PC)
        Map<String, Object> memory = new HashMap<>();
        int pc = 0;
        int cycles = 0;

        // Se houver um rótulo "main", inicia a execução a partir dele, caso contrário do início
        if (labelMap.containsKey("main")) {
            pc = labelMap.get("main");
        }

        while (pc >= 0 && pc < program.size()) {
            cycles++;
            if (cycles > maxCycles) {
                throw new RuntimeException("Limite de ciclos de execução excedido (" + maxCycles 
                        + "). Possível laço infinito no código interpretado!");
            }

            TACInstruction inst = program.get(pc);

            switch (inst.kind) {
                case LABEL, COMMENT -> {
                    // Marcador de rótulo / comentário é no-op em tempo de execução
                    pc++;
                }

                case GOTO -> {
                    if (!labelMap.containsKey(inst.label)) {
                        throw new RuntimeException("Erro em tempo de execução: rótulo não encontrado: " + inst.label);
                    }
                    pc = labelMap.get(inst.label);
                }

                case IF_FALSE -> {
                    Object condVal = resolveValue(inst.op1, memory);
                    boolean isFalse = isFalsy(condVal);
                    if (isFalse) {
                        if (!labelMap.containsKey(inst.label)) {
                            throw new RuntimeException("Erro em tempo de execução: rótulo não encontrado: " + inst.label);
                        }
                        pc = labelMap.get(inst.label);
                    } else {
                        pc++;
                    }
                }

                case IF_TRUE -> {
                    Object condVal = resolveValue(inst.op1, memory);
                    boolean isTrue = !isFalsy(condVal);
                    if (isTrue) {
                        if (!labelMap.containsKey(inst.label)) {
                            throw new RuntimeException("Erro em tempo de execução: rótulo não encontrado: " + inst.label);
                        }
                        pc = labelMap.get(inst.label);
                    } else {
                        pc++;
                    }
                }

                case PRINT -> {
                    Object val = resolveValue(inst.op1, memory);
                    String text = formatPrint(val);
                    result.addOutput(text);
                    if (printToConsole) {
                        System.out.println(text);
                    }
                    pc++;
                }

                case RETURN -> {
                    if (inst.op1 != null && !inst.op1.isEmpty()) {
                        Object val = resolveValue(inst.op1, memory);
                        if (val instanceof Number num) {
                            result.setReturnValue(num.intValue());
                        }
                    }
                    // Retorno encerra a execução do programa
                    pc = program.size();
                }

                case ASSIGN_COPY -> {
                    Object val = resolveValue(inst.op1, memory);
                    memory.put(inst.dest, val);
                    result.setVariable(inst.dest, val);
                    pc++;
                }

                case ASSIGN_UNARY -> {
                    Object val = resolveValue(inst.op1, memory);
                    Number number = val instanceof Number n ? n : 0;
                    Object res = switch (inst.op) {
                        case "float" -> number.doubleValue();
                        case "!" -> number.doubleValue() == 0.0 ? 1 : 0;
                        case "-" -> val instanceof Double || val instanceof Float
                                ? -number.doubleValue() : -number.intValue();
                        default -> throw new UnsupportedOperationException(
                                "Operador unário desconhecido: " + inst.op);
                    };
                    memory.put(inst.dest, res);
                    result.setVariable(inst.dest, res);
                    pc++;
                }

                case ASSIGN_BIN -> {
                    Object leftVal = resolveValue(inst.op1, memory);
                    Object rightVal = resolveValue(inst.op2, memory);

                    Number leftNumber = leftVal instanceof Number n ? n : 0;
                    Number rightNumber = rightVal instanceof Number n ? n : 0;
                    boolean floatingOperation = leftNumber instanceof Double || leftNumber instanceof Float
                            || rightNumber instanceof Double || rightNumber instanceof Float;
                    Object res = floatingOperation
                            ? evaluateFloatingOperation(inst.op, leftNumber.doubleValue(), rightNumber.doubleValue())
                            : evaluateIntegerOperation(inst.op, leftNumber.intValue(), rightNumber.intValue());

                    memory.put(inst.dest, res);
                    result.setVariable(inst.dest, res);
                    pc++;
                }
            }
        }

        result.setInstructionsExecuted(cycles);
        return result;
    }

    /**
     * Resolve um valor literal (inteiro ou string) ou obtém da memória de variáveis.
     */
    private Object resolveValue(String place, Map<String, Object> memory) {
        if (place == null) return 0;
        String trimmed = place.trim();

        // String literal entre aspas
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        // Inteiro literal
        if (TACInstruction.isInteger(trimmed)) {
            return Integer.parseInt(trimmed);
        }
        if (TACInstruction.isNumeric(trimmed)) {
            return Double.parseDouble(trimmed);
        }

        // Consulta na memória
        if (memory.containsKey(trimmed)) {
            return memory.get(trimmed);
        }

        // Variável não encontrada na memória padrão 0
        return 0;
    }

    /**
     * Determina se um valor é falsy (0 ou false).
     */
    private boolean isFalsy(Object val) {
        if (val == null) return true;
        if (val instanceof Number n) {
            return n.doubleValue() == 0.0;
        }
        if (val instanceof Boolean b) {
            return !b;
        }
        if (val instanceof String s) {
            return s.isEmpty();
        }
        return false;
    }

    private String formatPrint(Object val) {
        if (val == null) return "0";
        return val.toString();
    }

    private Object evaluateIntegerOperation(String op, int left, int right) {
        return switch (op) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) throw new ArithmeticException("Divisão por zero em tempo de execução!");
                yield left / right;
            }
            case "<" -> left < right ? 1 : 0;
            case ">" -> left > right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            case "==" -> left == right ? 1 : 0;
            case "!=" -> left != right ? 1 : 0;
            case "&&" -> left != 0 && right != 0 ? 1 : 0;
            case "||" -> left != 0 || right != 0 ? 1 : 0;
            default -> throw new UnsupportedOperationException("Operador desconhecido: " + op);
        };
    }

    private Object evaluateFloatingOperation(String op, double left, double right) {
        return switch (op) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0.0) throw new ArithmeticException("Divisão por zero em tempo de execução!");
                yield left / right;
            }
            case "<" -> left < right ? 1 : 0;
            case ">" -> left > right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            case "==" -> left == right ? 1 : 0;
            case "!=" -> left != right ? 1 : 0;
            case "&&" -> left != 0.0 && right != 0.0 ? 1 : 0;
            case "||" -> left != 0.0 || right != 0.0 ? 1 : 0;
            default -> throw new UnsupportedOperationException("Operador desconhecido: " + op);
        };
    }
}
