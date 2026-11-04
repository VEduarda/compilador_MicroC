package optimizer;

import optimizer.TACInstruction.Kind;

import java.util.*;

/**
 * Módulo de Otimização de Código Intermediário (Item 5 do Enunciado).
 *
 * Aplica técnicas clássicas de otimização sobre a representação linear TAC (IR),
 * reduzindo operações desnecessárias e melhorando a eficiência sem alterar
 * a semântica observável do programa.
 *
 * Técnicas Implementadas:
 *   1. Avaliação de Constantes (Constant Folding)
 *   2. Propagação de Constantes (Constant Propagation)
 *   3. Propagação de Cópias (Copy Propagation)
 *   4. Simplificação de Expressões / Identidades Algébricas (Algebraic Simplification)
 *   5. Eliminação de Subexpressões Comuns (Common Subexpression Elimination - CSE)
 *   6. Eliminação de Código Morto (Dead Code Elimination - DCE):
 *        - Remoção de temporárias nunca lidas
 *        - Remoção de instruções inalcançáveis após goto/return
 *        - Remoção de saltos redundantes (goto L seguido de L:)
 *        - Remoção de autoatribuições (x = x)
 */
public class TACOptimizer {

    private final OptimizationReport report = new OptimizationReport();

    public OptimizationReport getReport() {
        return report;
    }

    /**
     * Otimiza uma lista de instruções TAC textuais até convergência (ponto fixo).
     */
    public List<String> optimize(List<String> rawInstructions) {
        report.setOriginalInstructions(rawInstructions);

        // Converte strings em lista estruturada de TACInstruction
        List<TACInstruction> program = new ArrayList<>();
        for (String raw : rawInstructions) {
            TACInstruction inst = TACInstruction.parse(raw);
            if (inst != null && inst.kind != Kind.COMMENT) {
                program.add(inst);
            }
        }

        // Aplica sucessivas passadas até que nenhuma transformação adicional ocorra
        int maxPasses = 15;
        int pass = 0;
        boolean changed = true;

        while (changed && pass < maxPasses) {
            pass++;
            changed = false;

            // 1. Constant Folding & Propagação dentro de blocos básicos
            if (passConstantFoldingAndPropagation(program)) {
                changed = true;
            }

            // 2. Simplificações Algébricas
            if (passAlgebraicSimplification(program)) {
                changed = true;
            }

            // 3. Eliminação de Subexpressões Comuns (CSE)
            if (passCommonSubexpressionElimination(program)) {
                changed = true;
            }

            // 4. Propagação de Cópias
            if (passCopyPropagation(program)) {
                changed = true;
            }

            // 5. Eliminação de Código Inalcançável e Saltos Redundantes
            if (passControlFlowOptimization(program)) {
                changed = true;
            }

            // 6. Eliminação de Código Morto (Temporárias não lidas e autoatribuições)
            if (passDeadCodeElimination(program)) {
                changed = true;
            }
        }

        // Converte o programa otimizado de volta para formato textual
        List<String> result = new ArrayList<>();
        for (TACInstruction inst : program) {
            result.add(inst.toTAC());
        }

        report.setOptimizedInstructions(result);
        return result;
    }

    // =========================================================================
    // 1. Constant Folding & Constant Propagation
    // =========================================================================

    private boolean passConstantFoldingAndPropagation(List<TACInstruction> program) {
        boolean anyChange = false;

        // Propaga constantes conhecidas ao longo do fluxo básico
        Map<String, String> constMap = new HashMap<>();

        for (int i = 0; i < program.size(); i++) {
            TACInstruction inst = program.get(i);

            // Rótulo ou salto invalida o contexto de fluxo sequencial estrito
            if (inst.kind == Kind.LABEL || inst.kind == Kind.GOTO 
                    || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || inst.kind == Kind.CALL) {
                constMap.clear();
                continue;
            }

            String beforeText = inst.toTAC();

            // A) Substitui operandos por constantes já conhecidas
            if (inst.kind == Kind.ASSIGN_BIN) {
                if (constMap.containsKey(inst.op1)) {
                    String oldVal = inst.op1;
                    inst.op1 = constMap.get(inst.op1);
                    report.addTransformation("Constant Propagation", beforeText, inst.toTAC(),
                            "Substituído operando '" + oldVal + "' pelo valor constante " + inst.op1);
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
                if (constMap.containsKey(inst.op2)) {
                    String oldVal = inst.op2;
                    inst.op2 = constMap.get(inst.op2);
                    report.addTransformation("Constant Propagation", beforeText, inst.toTAC(),
                            "Substituído operando '" + oldVal + "' pelo valor constante " + inst.op2);
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
            } else if (inst.kind == Kind.ASSIGN_UNARY || inst.kind == Kind.ASSIGN_COPY 
                    || inst.kind == Kind.PRINT || inst.kind == Kind.RETURN
                    || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || inst.kind == Kind.PARAM) {
                if (inst.op1 != null && constMap.containsKey(inst.op1)) {
                    String oldVal = inst.op1;
                    inst.op1 = constMap.get(inst.op1);
                    report.addTransformation("Constant Propagation", beforeText, inst.toTAC(),
                            "Substituído operando '" + oldVal + "' pelo valor constante " + inst.op1);
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
            }

            // B) Constant Folding em Operações Binárias (ex.: t1 = 10 * 2 -> t1 = 20)
            if (inst.kind == Kind.ASSIGN_BIN && TACInstruction.isInteger(inst.op1) && TACInstruction.isInteger(inst.op2)) {
                int left = Integer.parseInt(inst.op1);
                int right = Integer.parseInt(inst.op2);
                String opName = inst.op;
                Integer res = evalBinOp(inst.op, left, right);

                if (res != null) {
                    inst.kind = Kind.ASSIGN_COPY;
                    inst.op1 = String.valueOf(res);
                    inst.op = null;
                    inst.op2 = null;
                    report.addTransformation("Constant Folding", beforeText, inst.toTAC(),
                            "Dobramento de constantes: " + left + " " + opName + " " + right + " = " + res);
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
            }

            // C) Constant Folding em Operações Unárias (ex.: t1 = - 5 -> t1 = -5, t1 = ! 0 -> t1 = 1)
            if (inst.kind == Kind.ASSIGN_UNARY && inst.op.equals("float")
                    && TACInstruction.isNumeric(inst.op1)) {
                double val = Double.parseDouble(inst.op1);
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = Double.toString(val);
                inst.op = null;
                report.addTransformation("Numeric Conversion", beforeText, inst.toTAC(),
                        "Conversão de literal inteiro para float");
                beforeText = inst.toTAC();
                anyChange = true;
            } else if (inst.kind == Kind.ASSIGN_UNARY && TACInstruction.isInteger(inst.op1)) {
                int val = Integer.parseInt(inst.op1);
                int res = inst.op.equals("!") ? (val == 0 ? 1 : 0) : -val;
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = String.valueOf(res);
                inst.op = null;
                report.addTransformation("Constant Folding", beforeText, inst.toTAC(),
                        "Dobramento unário: " + inst.op + val + " = " + res);
                beforeText = inst.toTAC();
                anyChange = true;
            }

            // D) Registra constante atribuída
            if (inst.kind == Kind.ASSIGN_COPY) {
                if (TACInstruction.isNumeric(inst.op1)) {
                    constMap.put(inst.dest, inst.op1);
                } else {
                    constMap.remove(inst.dest);
                }
            } else if (inst.getDef() != null) {
                constMap.remove(inst.getDef());
            }
        }

        return anyChange;
    }

    // =========================================================================
    // 2. Simplificações Algébricas
    // =========================================================================

    private static boolean isZero(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        return trimmed.equals("0") || trimmed.equals("0.0");
    }

    private static boolean isOne(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        return trimmed.equals("1") || trimmed.equals("1.0");
    }

    private boolean passAlgebraicSimplification(List<TACInstruction> program) {
        boolean anyChange = false;

        for (TACInstruction inst : program) {
            if (inst.kind != Kind.ASSIGN_BIN) continue;

            String beforeText = inst.toTAC();
            String left = inst.op1;
            String op = inst.op;
            String right = inst.op2;

            // a + 0 -> a
            if (op.equals("+") && isZero(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade aditiva: " + left + " + 0 = " + left);
                anyChange = true;
            }
            // 0 + b -> b
            else if (op.equals("+") && isZero(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = right;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade aditiva: 0 + " + right + " = " + right);
                anyChange = true;
            }
            // e - 0 -> e
            else if (op.equals("-") && isZero(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade subtrativa: " + left + " - 0 = " + left);
                anyChange = true;
            }
            // x - x -> 0
            else if (op.equals("-") && left.equals(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Subtração de mesmo termo: " + left + " - " + right + " = 0");
                anyChange = true;
            }
            // c * 1 -> c
            else if (op.equals("*") && isOne(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade multiplicativa: " + left + " * 1 = " + left);
                anyChange = true;
            }
            // 1 * c -> c
            else if (op.equals("*") && isOne(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = right;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade multiplicativa: 1 * " + right + " = " + right);
                anyChange = true;
            }
            // d * 0 -> 0 ou 0 * d -> 0
            else if (op.equals("*") && (isZero(right) || isZero(left))) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Anulação multiplicativa por zero = 0");
                anyChange = true;
            }
            // f / 1 -> f
            else if (op.equals("/") && isOne(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Divisão por 1: " + left + " / 1 = " + left);
                anyChange = true;
            }
            // 0 / x -> 0 (quando x != 0)
            else if (op.equals("/") && isZero(left) && !isZero(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Divisão de zero por número não-nulo = 0");
                anyChange = true;
            }
            // x / x -> 1 (quando x != 0)
            else if (op.equals("/") && left.equals(right) && !isZero(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "1";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Divisão de mesmo termo = 1");
                anyChange = true;
            }
            // Comparações reflexivas: x == x -> 1, x != x -> 0
            else if (op.equals("==") && left.equals(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "1";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Comparação reflexiva: " + left + " == " + right + " = 1");
                anyChange = true;
            } else if (op.equals("!=") && left.equals(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Comparação reflexiva: " + left + " != " + right + " = 0");
                anyChange = true;
            }
            // Identidades booleanas: x && 1 -> x, x && 0 -> 0, x || 1 -> 1, x || 0 -> x
            else if (op.equals("&&") && isZero(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Conjunção com falso: " + left + " && 0 = 0");
                anyChange = true;
            } else if (op.equals("&&") && isZero(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "0";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Conjunção com falso: 0 && " + right + " = 0");
                anyChange = true;
            } else if (op.equals("&&") && isOne(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade booleana: " + left + " && 1 = " + left);
                anyChange = true;
            } else if (op.equals("&&") && isOne(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = right;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade booleana: 1 && " + right + " = " + right);
                anyChange = true;
            } else if (op.equals("||") && isOne(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "1";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Disjunção com verdadeiro: " + left + " || 1 = 1");
                anyChange = true;
            } else if (op.equals("||") && isOne(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = "1";
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Disjunção com verdadeiro: 1 || " + right + " = 1");
                anyChange = true;
            } else if (op.equals("||") && isZero(right)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = left;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade booleana: " + left + " || 0 = " + left);
                anyChange = true;
            } else if (op.equals("||") && isZero(left)) {
                inst.kind = Kind.ASSIGN_COPY;
                inst.op1 = right;
                inst.op = null;
                inst.op2 = null;
                report.addTransformation("Algebraic Simplification", beforeText, inst.toTAC(),
                        "Identidade booleana: 0 || " + right + " = " + right);
                anyChange = true;
            }
        }

        return anyChange;
    }

    // =========================================================================
    // 3. Common Subexpression Elimination (CSE)
    // =========================================================================

    private boolean passCommonSubexpressionElimination(List<TACInstruction> program) {
        boolean anyChange = false;

        // Chave da subexpressão: "op1 op op2" -> nome da variável que já possui esse cálculo
        Map<String, String> exprMap = new HashMap<>();

        for (TACInstruction inst : program) {
            // Limites de blocos básicos invalidam o mapeamento
            if (inst.kind == Kind.LABEL || inst.kind == Kind.GOTO 
                    || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || inst.kind == Kind.CALL) {
                exprMap.clear();
                continue;
            }

            if (inst.kind == Kind.ASSIGN_BIN) {
                String exprKey = canonicalExprKey(inst.op1, inst.op, inst.op2);
                String beforeText = inst.toTAC();

                if (exprMap.containsKey(exprKey)) {
                    // Já calculada anteriormente: reaproveita a variável existente!
                    String existingVar = exprMap.get(exprKey);
                    inst.kind = Kind.ASSIGN_COPY;
                    inst.op1 = existingVar;
                    inst.op = null;
                    inst.op2 = null;
                    report.addTransformation("Common Subexpression Elimination", beforeText, inst.toTAC(),
                            "Expressão comum '" + exprKey + "' já calculada em '" + existingVar + "'");
                    anyChange = true;

                    // O destino foi redefinido; invalida expressões que dependiam dele antes
                    invalidateExprMap(exprMap, inst.dest);
                } else {
                    // Invalida quaisquer expressões que usavam inst.dest como operando ou resultado antes
                    invalidateExprMap(exprMap, inst.dest);

                    // Só registra no exprMap se o destino não sobrescreve os próprios operandos (ex.: x = x + 1)
                    if (!inst.dest.equals(inst.op1) && !inst.dest.equals(inst.op2)) {
                        exprMap.put(exprKey, inst.dest);
                    }
                }
            } else {
                // Outras instruções com destino (ASSIGN_COPY, ASSIGN_UNARY) invalidam mapeamentos que usam esse destino
                String def = inst.getDef();
                if (def != null) {
                    invalidateExprMap(exprMap, def);
                }
            }
        }

        return anyChange;
    }

    private String canonicalExprKey(String op1, String op, String op2) {
        if (op.equals("+") || op.equals("*") || op.equals("==") || op.equals("!=")) {
            if (op1.compareTo(op2) > 0) {
                return op2 + " " + op + " " + op1;
            }
        }
        return op1 + " " + op + " " + op2;
    }

    private void invalidateExprMap(Map<String, String> exprMap, String varName) {
        if (varName == null) return;
        exprMap.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split(" ");
            return parts[0].equals(varName) 
                    || (parts.length > 2 && parts[2].equals(varName)) 
                    || entry.getValue().equals(varName);
        });
    }

    // =========================================================================
    // 4. Copy Propagation (Propagação de Cópias)
    // =========================================================================

    private boolean passCopyPropagation(List<TACInstruction> program) {
        boolean anyChange = false;
        Map<String, String> copyMap = new HashMap<>();

        for (TACInstruction inst : program) {
            if (inst.kind == Kind.LABEL || inst.kind == Kind.GOTO 
                    || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || inst.kind == Kind.CALL) {
                copyMap.clear();
                continue;
            }

            String beforeText = inst.toTAC();

            if (inst.kind == Kind.ASSIGN_BIN) {
                if (copyMap.containsKey(inst.op1)) {
                    String oldVal = inst.op1;
                    inst.op1 = copyMap.get(inst.op1);
                    report.addTransformation("Copy Propagation", beforeText, inst.toTAC(),
                            "Propagação de cópia: substituído '" + oldVal + "' por '" + inst.op1 + "'");
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
                if (copyMap.containsKey(inst.op2)) {
                    String oldVal = inst.op2;
                    inst.op2 = copyMap.get(inst.op2);
                    report.addTransformation("Copy Propagation", beforeText, inst.toTAC(),
                            "Propagação de cópia: substituído '" + oldVal + "' por '" + inst.op2 + "'");
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
            } else if (inst.kind == Kind.ASSIGN_UNARY || inst.kind == Kind.ASSIGN_COPY 
                    || inst.kind == Kind.PRINT || inst.kind == Kind.RETURN
                    || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || inst.kind == Kind.PARAM) {
                if (inst.op1 != null && copyMap.containsKey(inst.op1)) {
                    String oldVal = inst.op1;
                    inst.op1 = copyMap.get(inst.op1);
                    report.addTransformation("Copy Propagation", beforeText, inst.toTAC(),
                            "Propagação de cópia: substituído '" + oldVal + "' por '" + inst.op1 + "'");
                    beforeText = inst.toTAC();
                    anyChange = true;
                }
            }

            // Registra cópia direta: dest = op1
            if (inst.kind == Kind.ASSIGN_COPY) {
                if (TACInstruction.isVariable(inst.op1)) {
                    copyMap.put(inst.dest, inst.op1);
                } else {
                    copyMap.remove(inst.dest);
                }
            } else if (inst.getDef() != null) {
                copyMap.remove(inst.getDef());
                // Invalida também onde dest era o valor fonte de uma cópia
                copyMap.entrySet().removeIf(entry -> entry.getValue().equals(inst.getDef()));
            }
        }

        return anyChange;
    }

    // =========================================================================
    // 5. Otimização de Fluxo de Controle (Saltos Redundantes e Inalcançáveis)
    // =========================================================================

    private boolean passControlFlowOptimization(List<TACInstruction> program) {
        boolean anyChange = false;

        for (int i = 0; i < program.size(); i++) {
            TACInstruction inst = program.get(i);

            // A) Salto condicional com condição constante (ifFalse 0 goto L, ifFalse 1 goto L)
            if (inst.kind == Kind.IF_FALSE && TACInstruction.isInteger(inst.op1)) {
                int condVal = Integer.parseInt(inst.op1);
                String beforeText = inst.toTAC();
                if (condVal == 0) {
                    // Condição é sempre 0 (falso): o salto é SEMPRE executado! Transforma em goto incondicional.
                    inst.kind = Kind.GOTO;
                    inst.op1 = null;
                    report.addTransformation("Constant Condition Jump", beforeText, inst.toTAC(),
                            "Condição avaliada como sempre falsa (0): salto sempre tomado");
                    anyChange = true;
                } else {
                    // Condição é não-zero (verdadeiro): o salto NUNCA é executado! Pode remover a instrução.
                    program.remove(i);
                    i--;
                    report.addTransformation("Dead Branch Elimination", beforeText, "",
                            "Condição avaliada como sempre verdadeira (" + condVal + "): salto nunca tomado");
                    anyChange = true;
                    continue;
                }
            }

            // B) Salto incondicional imediatamente seguido pelo próprio rótulo destino:
            //    goto L1
            //    L1:
            if (inst.kind == Kind.GOTO && i + 1 < program.size()) {
                TACInstruction next = program.get(i + 1);
                if (next.kind == Kind.LABEL && next.label.equals(inst.label)) {
                    String beforeText = inst.toTAC();
                    program.remove(i);
                    i--;
                    report.addTransformation("Redundant Jump Elimination", beforeText, "",
                            "Salto incondicional aponta diretamente para a próxima instrução (" + next.label + ":)");
                    anyChange = true;
                    continue;
                }
            }

            // C) Código inalcançável: instruções entre um goto incondicional e o próximo rótulo
            if (inst.kind == Kind.GOTO && i + 1 < program.size()) {
                TACInstruction next = program.get(i + 1);
                if (next.kind != Kind.LABEL) {
                    String beforeText = next.toTAC();
                    program.remove(i + 1);
                    i--;
                    report.addTransformation("Unreachable Code Elimination", beforeText, "",
                            "Instrução inalcançável imediatamente após salto incondicional 'goto " + inst.label + "'");
                    anyChange = true;
                }
            }
        }

        return anyChange;
    }

    // =========================================================================
    // 6. Eliminação de Código Morto (DCE)
    // =========================================================================

    private boolean passDeadCodeElimination(List<TACInstruction> program) {
        boolean anyChange = false;

        // Coleta o conjunto de todas as variáveis e temporárias lidas em qualquer lugar do programa
        Set<String> usedVariables = new HashSet<>();
        for (TACInstruction inst : program) {
            usedVariables.addAll(inst.getUses());
        }

        // Verifica se o programa possui comandos com efeitos observáveis (prints, retornos, desvios ou variáveis do usuário)
        boolean hasRoots = false;
        for (TACInstruction inst : program) {
            if (inst.kind == Kind.PRINT || inst.kind == Kind.RETURN 
                    || inst.kind == Kind.GOTO || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE
                    || (inst.getDef() != null && !TACInstruction.isTemporary(inst.getDef()))) {
                hasRoots = true;
                break;
            }
        }

        for (int i = 0; i < program.size(); i++) {
            TACInstruction inst = program.get(i);

            // A) Autoatribuição: x = x
            if (inst.kind == Kind.ASSIGN_COPY && inst.dest.equals(inst.op1)) {
                String beforeText = inst.toTAC();
                program.remove(i);
                i--;
                report.addTransformation("Self-Assignment Elimination", beforeText, "",
                        "Autoatribuição redundante '" + beforeText + "' removida");
                anyChange = true;
                continue;
            }

            // B) Temporária nunca lida: t = ...
            // Temporárias geradas pelo compilador (t1, t2, ...) que não constam no conjunto de usos são código morto
            // apenas quando o programa possui raízes de efeitos observáveis (em trechos isolados de temporárias,
            // elas representam o próprio resultado sintetizado).
            if ((inst.kind == Kind.ASSIGN_BIN || inst.kind == Kind.ASSIGN_UNARY || inst.kind == Kind.ASSIGN_COPY)
                    && TACInstruction.isTemporary(inst.dest)) {

                if (hasRoots && !usedVariables.contains(inst.dest)) {
                    String beforeText = inst.toTAC();
                    program.remove(i);
                    i--;
                    report.addTransformation("Dead Code Elimination", beforeText, "",
                            "Temporária '" + inst.dest + "' nunca é lida posteriormente no programa");
                    anyChange = true;
                    continue;
                }

                // C) Sobrescrita morta: se a temporária é redefinida antes de ser lida no bloco básico
                boolean readBeforeRedefine = false;
                boolean redefined = false;
                for (int j = i + 1; j < program.size(); j++) {
                    TACInstruction future = program.get(j);
                    if (future.kind == Kind.LABEL || future.kind == Kind.GOTO 
                            || future.kind == Kind.IF_FALSE || future.kind == Kind.IF_TRUE) {
                        break;
                    }
                    if (future.getUses().contains(inst.dest)) {
                        readBeforeRedefine = true;
                        break;
                    }
                    if (inst.dest.equals(future.getDef())) {
                        redefined = true;
                        break;
                    }
                }

                if (redefined && !readBeforeRedefine) {
                    String beforeText = inst.toTAC();
                    program.remove(i);
                    i--;
                    report.addTransformation("Dead Definition Elimination", beforeText, "",
                            "Temporária '" + inst.dest + "' é sobrescrita antes de qualquer leitura");
                    anyChange = true;
                    continue;
                }
            }
        }

        // D) Rótulos não referenciados por nenhum desvio (exceto 'main')
        Set<String> targetedLabels = new HashSet<>();
        for (TACInstruction inst : program) {
            if (inst.kind == Kind.GOTO || inst.kind == Kind.IF_FALSE || inst.kind == Kind.IF_TRUE) {
                if (inst.label != null) targetedLabels.add(inst.label);
            }
            if (inst.kind == Kind.CALL && inst.op1 != null) {
                targetedLabels.add(inst.op1);
            }
        }

        for (int i = 0; i < program.size(); i++) {
            TACInstruction inst = program.get(i);
            if (inst.kind == Kind.LABEL && !inst.label.equals("main") && !targetedLabels.contains(inst.label)) {
                String beforeText = inst.toTAC();
                program.remove(i);
                i--;
                report.addTransformation("Unused Label Elimination", beforeText, "",
                        "Rótulo '" + inst.label + ":' não possui nenhum salto apontando para ele");
                anyChange = true;
            }
        }

        return anyChange;
    }

    // =========================================================================
    // Auxiliar: Cálculo aritmético e relacional
    // =========================================================================

    private Integer evalBinOp(String op, int left, int right) {
        if (op == null) return null;
        return switch (op) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> (right != 0) ? (left / right) : null;
            case "<" -> (left < right) ? 1 : 0;
            case ">" -> (left > right) ? 1 : 0;
            case "<=" -> (left <= right) ? 1 : 0;
            case ">=" -> (left >= right) ? 1 : 0;
            case "==" -> (left == right) ? 1 : 0;
            case "!=" -> (left != right) ? 1 : 0;
            case "&&" -> ((left != 0) && (right != 0)) ? 1 : 0;
            case "||" -> ((left != 0) || (right != 0)) ? 1 : 0;
            default -> null;
        };
    }
}
