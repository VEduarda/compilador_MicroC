package sdt;

import ast.Nodes.ProgramNode;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executor da Bateria de Testes Obrigatórios e de Validação (Itens 1 a 21).
 *
 * Demonstra a execução dos cenários avaliativos:
 *   - Teste 1: Precedência Aritmética (int x = 2 + 3 * 4; print(x);)
 *   - Teste 2: Parênteses Aninhados (int y = (2 + 3) * (10 - 4);)
 *   - Teste 3: Condicional If-Else (if (a > 10) { print(a); } else { print(0); })
 *   - Teste 4: Laço While com Acumulador (int i = 0; while (i < 5) { i = i + 1; print(i); })
 *   - Teste 5: Melhoria 2: Reuso Inteligente de Variáveis Temporárias
 *   - Teste 6: Melhoria 1/3: Constant Folding / Dobramento de Constantes
 *   - Teste 7: Melhoria 5: Break Simples dentro de While condicionado por If
 *   - Teste 8: Melhoria 5: Continue Simples dentro de While condicionado por If
 *   - Teste 9: Melhoria 5: Break e Continue no Mesmo Laço
 *   - Teste 10: Melhoria 5: Laços Aninhados (Break no Laço Interno)
 *   - Teste 11: Melhoria 5: Validação Semântica / Erro para Break/Continue Fora de Laço
 *   - Teste 12: Melhoria 5: Integração com Constant Folding e Reuso de Temporárias
 *   - Teste 13: Melhoria 5: Break após While já encerrado
 *   - Teste 14: Melhoria 5: Dois Whiles sequenciais com seus respectivos breaks
 *   - Teste 15: Melhoria 5: Continue dentro de dois Ifs aninhados dentro de While
 *   - Teste 16: Caso de Borda 1: Break e Continue em While dentro de If/Else
 *   - Teste 17: Caso de Borda 2: While aninhado em 3 níveis com break no laço do meio
 *   - Teste 18: Caso de Borda 3: Continue como último comando do corpo do While
 *   - Teste 19: Caso de Borda 4: Break em While cuja condição usa Constant Folding
 *   - Teste 20: Caso de Borda 5: Expressões com várias temporárias e pool antes do break
 *   - Teste 21: Caso de Borda 6: Break fora de laço dentro do bloco Else
 *
 * Para cada cenário, executa o princípio "Uma AST, Três Traduções":
 *   1. Notação Polonesa Reversa (RPN) via PostfixTranslator
 *   2. Código Intermediário de Três Endereços (TAC / IR) via TACTranslator
 *   3. Código C Canônico Formatado via PrettyPrinter
 */
public class SDTTestRunner {

    private static int totalChecks = 0;
    private static int failedChecks = 0;

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            boolean success = runAllTests();
            if (!success || failedChecks > 0) {
                System.err.printf("%n[FALHA] %d de %d verificações falharam na bateria SDT.%n",
                        failedChecks, totalChecks);
                System.exit(1);
            }
        } catch (Throwable t) {
            System.err.println("\n[ERRO CRÍTICO DE EXECUÇÃO]: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static void resetMetrics() {
        totalChecks = 0;
        failedChecks = 0;
    }

    public static void check(boolean condition, String description) {
        totalChecks++;
        if (condition) {
            System.out.println("  [OK] " + description);
        } else {
            failedChecks++;
            System.out.println("  [FALHA] " + description);
        }
    }

    public static int getTotalChecks() {
        return totalChecks;
    }

    public static int getFailedChecks() {
        return failedChecks;
    }

    public static boolean allGotosDefined(List<String> instructions) {
        Set<String> definedLabels = new HashSet<>();
        for (String inst : instructions) {
            String trimmed = inst.trim();
            if (trimmed.endsWith(":") && !trimmed.contains(" ")) {
                definedLabels.add(trimmed.substring(0, trimmed.length() - 1));
            }
        }
        for (String inst : instructions) {
            String trimmed = inst.trim();
            if (trimmed.startsWith("goto ")) {
                String target = trimmed.substring(5).trim();
                if (!definedLabels.contains(target)) return false;
            } else if (trimmed.startsWith("ifFalse ")) {
                int idx = trimmed.indexOf("goto ");
                if (idx != -1) {
                    String target = trimmed.substring(idx + 5).trim();
                    if (!definedLabels.contains(target)) return false;
                }
            }
        }
        return true;
    }

    public static boolean runAllTests() {
        resetMetrics();

        System.out.println("================================================================================");
        System.out.println("     PROJETO PRÁTICO: TRADUÇÃO DIRIGIDA POR SINTAXE (SDT) - MICROC");
        System.out.println("                   BATERIA DE TESTES E VALIDAÇÃO COMPLETA           ");
        System.out.println("================================================================================");

        try { runTest1(); } catch (Throwable t) { check(false, "Teste 1 abortado por exceção: " + t.getMessage()); }
        try { runTest2(); } catch (Throwable t) { check(false, "Teste 2 abortado por exceção: " + t.getMessage()); }
        try { runTest3(); } catch (Throwable t) { check(false, "Teste 3 abortado por exceção: " + t.getMessage()); }
        try { runTest4(); } catch (Throwable t) { check(false, "Teste 4 abortado por exceção: " + t.getMessage()); }
        try { runTest5(); } catch (Throwable t) { check(false, "Teste 5 abortado por exceção: " + t.getMessage()); }
        try { runTest6(); } catch (Throwable t) { check(false, "Teste 6 abortado por exceção: " + t.getMessage()); }
        try { runTest7(); } catch (Throwable t) { check(false, "Teste 7 abortado por exceção: " + t.getMessage()); }
        try { runTest8(); } catch (Throwable t) { check(false, "Teste 8 abortado por exceção: " + t.getMessage()); }
        try { runTest9(); } catch (Throwable t) { check(false, "Teste 9 abortado por exceção: " + t.getMessage()); }
        try { runTest10(); } catch (Throwable t) { check(false, "Teste 10 abortado por exceção: " + t.getMessage()); }
        try { runTest11(); } catch (Throwable t) { check(false, "Teste 11 abortado por exceção: " + t.getMessage()); }
        try { runTest12(); } catch (Throwable t) { check(false, "Teste 12 abortado por exceção: " + t.getMessage()); }
        try { runTest13(); } catch (Throwable t) { check(false, "Teste 13 abortado por exceção: " + t.getMessage()); }
        try { runTest14(); } catch (Throwable t) { check(false, "Teste 14 abortado por exceção: " + t.getMessage()); }
        try { runTest15(); } catch (Throwable t) { check(false, "Teste 15 abortado por exceção: " + t.getMessage()); }
        try { runTest16(); } catch (Throwable t) { check(false, "Teste 16 abortado por exceção: " + t.getMessage()); }
        try { runTest17(); } catch (Throwable t) { check(false, "Teste 17 abortado por exceção: " + t.getMessage()); }
        try { runTest18(); } catch (Throwable t) { check(false, "Teste 18 abortado por exceção: " + t.getMessage()); }
        try { runTest19(); } catch (Throwable t) { check(false, "Teste 19 abortado por exceção: " + t.getMessage()); }
        try { runTest20(); } catch (Throwable t) { check(false, "Teste 20 abortado por exceção: " + t.getMessage()); }
        try { runTest21(); } catch (Throwable t) { check(false, "Teste 21 abortado por exceção: " + t.getMessage()); }

        System.out.println("================================================================================");
        if (failedChecks == 0) {
            System.out.printf("   TODOS OS %d CHECKS EXECUTADOS COM SUCESSO! (0 FALHAS DETECTADAS)%n", totalChecks);
        } else {
            System.out.printf("   ATENÇÃO: %d DE %d CHECKS FALHARAM!%n", failedChecks, totalChecks);
        }
        System.out.println("================================================================================\n");

        return failedChecks == 0;
    }

    /**
     * TESTE 1: Precedência Aritmética
     * Entrada: int x = 2 + 3 * 4; print(x);
     */
    public static void runTest1() {
        printHeader("TESTE 1: Precedência Aritmética");
        String code = "int x = 2 + 3 * 4; print(x);";
        System.out.println("Código MicroC de Entrada:\n" + code + "\n");

        ProgramNode ast = parse(code);

        // 1. PostfixTranslator (RPN)
        PostfixTranslator postfix = new PostfixTranslator();
        postfix.translate(ast);
        System.out.println("--- 1. NOTAÇÃO POLONESA REVERSA (RPN) ---");
        System.out.println("RPN das Expressões : " + String.join(", ", postfix.getExpressionRPNs()));
        System.out.println("Fluxo RPN Total    : " + postfix.getOutput());

        // 2. TACTranslator (TAC / IR)
        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- 2. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        // 3. PrettyPrinter
        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 3. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(inst.contains("x = 14") && inst.contains("print x"), "Precedência aritmética e Constant Folding calculados corretamente (x = 14)");

        printSeparator();
    }

    /**
     * TESTE 2: Parênteses Aninhados
     * Entrada: int y = (2 + 3) * (10 - 4);
     */
    public static void runTest2() {
        printHeader("TESTE 2: Parênteses Aninhados");
        String code = "int y = (2 + 3) * (10 - 4);";
        System.out.println("Código MicroC de Entrada:\n" + code + "\n");

        ProgramNode ast = parse(code);

        PostfixTranslator postfix = new PostfixTranslator();
        postfix.translate(ast);
        System.out.println("--- 1. NOTAÇÃO POLONESA REVERSA (RPN) ---");
        System.out.println("RPN das Expressões : " + String.join(", ", postfix.getExpressionRPNs()));

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- 2. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 3. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(inst.contains("y = 30"), "Parênteses aninhados e Constant Folding avaliados corretamente (y = 30)");

        printSeparator();
    }

    /**
     * TESTE 3: Condicional If-Else
     * Entrada: if (a > 10) { print(a); } else { print(0); }
     */
    public static void runTest3() {
        printHeader("TESTE 3: Condicional If-Else");
        String code = "if (a > 10) { print(a); } else { print(0); }";
        System.out.println("Código MicroC de Entrada:\n" + code + "\n");

        ProgramNode ast = parse(code);

        PostfixTranslator postfix = new PostfixTranslator();
        postfix.translate(ast);
        System.out.println("--- 1. NOTAÇÃO POLONESA REVERSA (RPN) ---");
        System.out.println("Fluxo RPN emitido: " + postfix.getOutput());

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- 2. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 3. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("ifFalse t1 goto L1") && inst.contains("goto L2"), "Contém salto condicional ifFalse e goto L_end");

        printSeparator();
    }

    /**
     * TESTE 4: Laço While com Acumulador
     */
    public static void runTest4() {
        printHeader("TESTE 4: Laço While com Acumulador");
        String code = """
        int i = 0;
        while (i < 5) {
            i = i + 1;
            print(i);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        PostfixTranslator postfix = new PostfixTranslator();
        postfix.translate(ast);
        System.out.println("--- 1. NOTAÇÃO POLONESA REVERSA (RPN) ---");
        System.out.println("RPN das Expressões: " + String.join(" | ", postfix.getExpressionRPNs()));

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- 2. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 3. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("L1:") && inst.contains("goto L1") && inst.contains("L2:"), "While possui rótulo L_start (L1:), retorno (goto L1) e saída (L2:)");

        printSeparator();
    }

    /**
     * TESTE 5: Demonstração da Melhoria 2 (Reuso Inteligente de Variáveis Temporárias)
     */
    public static void runTest5() {
        printHeader("TESTE 5: Demonstração da Melhoria 2 (Reuso Inteligente de Temporárias)");
        String code = """
        int a = (2 + 3) * (10 - 4);
        int b = (a + 5) * (a - 1);
        int c = a + b + 100;
        print(c);
        """;
        System.out.println("Código MicroC Complexo de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TempPool pool = new TempPool();
        TACTranslator tacWithPool = new TACTranslator(pool, true);
        List<String> instWithPool = tacWithPool.generate(ast);

        TACTranslator tacWithoutPool = new TACTranslator(null, false);
        List<String> instWithoutPool = tacWithoutPool.generate(ast);

        System.out.println("--- 1. COMPARAÇÃO DE CÓDIGO TAC GERADO ---");
        System.out.println("TAC COM POOL (Melhoria 2 Ativada)       | TAC SEM POOL (Geração Ingênua)");
        System.out.println("----------------------------------------+---------------------------------------");
        int maxLines = Math.max(instWithPool.size(), instWithoutPool.size());
        for (int i = 0; i < maxLines; i++) {
            String left = i < instWithPool.size() ? instWithPool.get(i) : "";
            String right = i < instWithoutPool.size() ? instWithoutPool.get(i) : "";
            System.out.printf("%-39s | %-39s%n", left, right);
        }

        System.out.println("\n" + pool.getSummaryReport());

        check(pool.getTotalReuses() > 0, "Pool de temporárias realizou reusos com sucesso");
        check(pool.getMaxCreated() < pool.getTotalAllocations(), "Pico máximo de temporárias é inferior ao total sem pool");

        printSeparator();
    }

    /**
     * TESTE 6: Demonstração da Melhoria 3 (Otimização de Constant Folding)
     */
    public static void runTest6() {
        printHeader("TESTE 6: Demonstração da Melhoria 3 (Constant Folding / Dobramento de Constantes)");
        String code = """
        int x = 2 + 3 * 4;
        int y = (2 + 3) * (10 - 4);
        int z = x + y;
        print(z);
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tacWithFolding = new TACTranslator(new TempPool(), true, true);
        List<String> instWithFolding = tacWithFolding.generate(ast);

        TACTranslator tacWithoutFolding = new TACTranslator(new TempPool(), true, false);
        List<String> instWithoutFolding = tacWithoutFolding.generate(ast);

        System.out.println("--- 1. COMPARAÇÃO DE CÓDIGO TAC: COM vs. SEM CONSTANT FOLDING ---");
        System.out.println("TAC COM CONSTANT FOLDING                | TAC SEM CONSTANT FOLDING (Geração Ingênua)");
        System.out.println("----------------------------------------+---------------------------------------");
        int maxLines = Math.max(instWithFolding.size(), instWithoutFolding.size());
        for (int i = 0; i < maxLines; i++) {
            String left = i < instWithFolding.size() ? instWithFolding.get(i) : "";
            String right = i < instWithoutFolding.size() ? instWithoutFolding.get(i) : "";
            System.out.printf("%-39s | %-39s%n", left, right);
        }

        int diff = instWithoutFolding.size() - instWithFolding.size();
        System.out.println("\n=== IMPACTO DA OTIMIZAÇÃO DE CONSTANT FOLDING ===");
        System.out.printf("  - Instruções sem otimização : %d%n", instWithoutFolding.size());
        System.out.printf("  - Instruções com otimização : %d%n", instWithFolding.size());
        System.out.printf("  - Instruções economizadas   : %d%n", diff);

        check(instWithFolding.size() < instWithoutFolding.size(), "Constant Folding reduziu o número total de instruções intermediárias");

        printSeparator();
    }

    /**
     * TESTE 7: Break Simples dentro de While condicionado por If (Melhoria 5)
     */
    public static void runTest7() {
        printHeader("TESTE 7: Break Simples dentro de While condicionado por If (Melhoria 5)");
        String code = """
        int i = 0;
        while (i < 10) {
            i = i + 1;
            if (i == 5) {
                break;
            }
            print(i);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("--- 1. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 2. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("goto L2"), "Comando break emite 'goto L2' (L_end do laço)");
        check(inst.indexOf("goto L2") < inst.indexOf("L2:"), "O desvio de break está posicionado antes do rótulo L_end");
        check(inst.contains("goto L1"), "Final do corpo emite 'goto L1' (L_start do laço)");

        printSeparator();
    }

    /**
     * TESTE 8: Continue Simples dentro de While condicionado por If (Melhoria 5)
     */
    public static void runTest8() {
        printHeader("TESTE 8: Continue Simples dentro de While condicionado por If (Melhoria 5)");
        String code = """
        int i = 0;
        while (i < 10) {
            i = i + 1;
            if (i == 5) {
                continue;
            }
            print(i);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("--- 1. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 2. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("goto L1"), "Comando continue emite 'goto L1' (L_start do laço)");
        check(inst.indexOf("L1:") < inst.indexOf("ifFalse t1 goto L2"), "L_start está posicionado antes da avaliação da condição");

        printSeparator();
    }

    /**
     * TESTE 9: Break e Continue no Mesmo Laço (Melhoria 5)
     */
    public static void runTest9() {
        printHeader("TESTE 9: Break e Continue no Mesmo Laço (Melhoria 5)");
        String code = """
        int i = 0;
        while (i < 20) {
            i = i + 1;
            if (i == 3) {
                continue;
            }
            if (i == 8) {
                break;
            }
            print(i);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("--- 1. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 2. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("goto L1"), "Continue emite salto para L_start (L1)");
        check(inst.contains("goto L2"), "Break emite salto para L_end (L2)");

        printSeparator();
    }

    /**
     * TESTE 10: Laços Aninhados (Break no Laço Interno - Melhoria 5)
     */
    public static void runTest10() {
        printHeader("TESTE 10: Laços Aninhados (Break no Laço Interno - Melhoria 5)");
        String code = """
        int i = 0;
        int j = 0;
        while (i < 3) {
            i = i + 1;
            j = 0;
            while (j < 5) {
                j = j + 1;
                if (j == 2) {
                    break;
                }
                print(j);
            }
            print(i);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("--- 1. CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("\n--- 2. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("goto L4"), "Break do laço interno emite 'goto L4' (L_end do laço interno)");
        int idxInnerStart = inst.indexOf("L3:");
        int idxInnerEnd = inst.indexOf("L4:");
        boolean innerBreakDoesNotTargetOuterEnd = !inst.subList(idxInnerStart, idxInnerEnd).contains("goto L2");
        check(innerBreakDoesNotTargetOuterEnd, "Break do laço interno não afeta o laço externo (não salta para L2)");

        printSeparator();
    }

    /**
     * TESTE 11: Validação Semântica e TAC de Break/Continue Fora de Laço (Melhoria 5)
     */
    public static void runTest11() {
        printHeader("TESTE 11: Validação Semântica e TAC para Break/Continue Fora de Laço (Melhoria 5)");

        // Caso 1: Break no escopo global
        String code1 = """
        int x = 10;
        break;
        print(x);
        """;
        System.out.println("Caso 1 - Código MicroC com 'break' no escopo global:\n" + code1);
        ProgramNode ast1 = parse(code1);

        SemanticAnalyzer sa1 = new SemanticAnalyzer();
        List<SemanticError> errors1 = sa1.analyze(ast1);
        System.out.println("Erros semânticos detectados pelo SemanticAnalyzer:");
        for (SemanticError err : errors1) {
            System.out.println("  " + err);
        }

        boolean tac1Threw = false;
        try {
            TACTranslator tac1 = new TACTranslator();
            tac1.generate(ast1);
        } catch (RuntimeException e) {
            tac1Threw = true;
            System.out.println("Exceção capturada no TACTranslator: " + e.getMessage());
        }

        check(!errors1.isEmpty() && errors1.get(0).rule.equals("R8-BreakForaDeLaco") && errors1.get(0).line == 2,
                "SemanticAnalyzer reportou R8-BreakForaDeLaco na linha correta (linha 2)");
        check(tac1Threw, "TACTranslator lançou exceção controlada ao tentar gerar código para break fora de laço");

        // Caso 2: Continue dentro de If sem While
        String code2 = """
        int x = 5;
        if (x > 0) {
            continue;
        }
        """;
        System.out.println("\nCaso 2 - Código MicroC com 'continue' dentro de 'if' fora de 'while':\n" + code2);
        ProgramNode ast2 = parse(code2);

        SemanticAnalyzer sa2 = new SemanticAnalyzer();
        List<SemanticError> errors2 = sa2.analyze(ast2);
        System.out.println("Erros semânticos detectados pelo SemanticAnalyzer:");
        for (SemanticError err : errors2) {
            System.out.println("  " + err);
        }

        boolean tac2Threw = false;
        try {
            TACTranslator tac2 = new TACTranslator();
            tac2.generate(ast2);
        } catch (RuntimeException e) {
            tac2Threw = true;
            System.out.println("Exceção capturada no TACTranslator: " + e.getMessage());
        }

        check(!errors2.isEmpty() && errors2.get(0).rule.equals("R8-ContinueForaDeLaco") && errors2.get(0).line == 3,
                "SemanticAnalyzer reportou R8-ContinueForaDeLaco na linha correta (linha 3)");
        check(tac2Threw, "TACTranslator lançou exceção controlada ao tentar gerar código para continue fora de laço");

        printSeparator();
    }

    /**
     * TESTE 12: Integração de Laço com Constant Folding e Reuso de Temporárias (Melhorias 1, 2 e 5)
     */
    public static void runTest12() {
        printHeader("TESTE 12: Laço com Constant Folding e Reuso de Temporárias (Sem Regressão)");
        String code = """
        int x = 0;
        while (x < (2 + 3 * 2)) {
            int y = (x + 1) * (x + 2);
            x = x + 1;
            if (x == 5) {
                break;
            }
            print(y);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TempPool pool = new TempPool();
        TACTranslator tac = new TACTranslator(pool, true, true);
        List<String> inst = tac.generate(ast);

        System.out.println("--- 1. CÓDIGO DE TRÊS ENDEREÇOS (TAC) GERADO ---");
        tac.printInstructions();

        System.out.println("\n" + pool.getSummaryReport());

        PrettyPrinter pp = new PrettyPrinter();
        System.out.println("--- 2. CÓDIGO CANÔNICO (PRETTY-PRINTER) ---");
        System.out.print(pp.format(ast));

        check(inst.contains("t1 = x < 8"), "Constant Folding aplicado na condição do while (2 + 3 * 2 -> 8)");
        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(pool.getTotalReuses() > 0, "TempPool realizou reuso inteligente de registradores temporários");

        printSeparator();
    }

    /**
     * TESTE 13: Break após While já encerrado
     */
    public static void runTest13() {
        printHeader("TESTE 13: Break após While já Encerrado (Melhoria 5)");
        String code = """
        int i = 0;
        while (i < 3) {
            i = i + 1;
        }
        break;
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        System.out.println("Erros semânticos detectados:");
        for (SemanticError err : errors) {
            System.out.println("  " + err);
        }

        boolean semOk = !errors.isEmpty() && errors.stream().anyMatch(e -> e.rule.equals("R8-BreakForaDeLaco") && e.line == 5);
        check(semOk, "SemanticAnalyzer detecta R8-BreakForaDeLaco na linha 5 após o término do laço");

        boolean tacHandled = false;
        try {
            TACTranslator tac = new TACTranslator();
            tac.generate(ast);
        } catch (RuntimeException e) {
            tacHandled = e.getMessage().contains("break fora de laço") && !(e instanceof NullPointerException);
            System.out.println("Exceção capturada no TACTranslator: " + e.getMessage());
        }
        check(tacHandled, "TACTranslator lança exceção controlada indicando 'break fora de laço' (sem NPE ou EmptyStack)");

        printSeparator();
    }

    /**
     * TESTE 14: Dois While em Sequência, cada um com seu Break
     */
    public static void runTest14() {
        printHeader("TESTE 14: Dois Whiles em Sequência com Breaks Independentes (Melhoria 5)");
        String code = """
        int a = 0;
        while (a < 5) {
            a = a + 1;
            if (a == 2) {
                break;
            }
        }
        int b = 0;
        while (b < 5) {
            b = b + 1;
            if (b == 3) {
                break;
            }
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        check(errors.isEmpty(), "Análise semântica sem erros para múltiplos laços sequenciais");

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");

        // 1º While usa L1 (start) e L2 (end); 2º While usa L4 (start) e L5 (end)
        check(inst.contains("goto L2") && inst.contains("goto L5"), "Break do 1º while salta para L2 e break do 2º while salta para L5");
        check(inst.indexOf("goto L2") < inst.indexOf("L4:"), "Rótulos e desvios do 1º while estão isolados do 2º while");

        printSeparator();
    }

    /**
     * TESTE 15: Continue dentro de dois If aninhados dentro de um While
     */
    public static void runTest15() {
        printHeader("TESTE 15: Continue dentro de Ifs Aninhados em While (Melhoria 5)");
        String code = """
        int x = 0;
        while (x < 10) {
            x = x + 1;
            if (x > 2) {
                if (x < 8) {
                    continue;
                }
            }
            print(x);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        check(errors.isEmpty(), "Análise semântica sem erros para continue em múltiplos ifs aninhados");

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido");
        check(inst.contains("goto L1"), "Continue aninhado gera salto incondicional para o início do laço (goto L1)");

        printSeparator();
    }

    /**
     * TESTE 16: Break e Continue em While dentro de If/Else (Caso de Borda 1)
     */
    public static void runTest16() {
        printHeader("TESTE 16: Break e Continue em While dentro de If/Else (Caso de Borda 1)");
        String code = """
        int modo = 1;
        int k = 0;
        if (modo == 1) {
            while (k < 5) {
                k = k + 1;
                if (k == 3) {
                    break;
                }
            }
        } else {
            while (k < 10) {
                k = k + 2;
                if (k == 6) {
                    continue;
                }
            }
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        check(errors.isEmpty(), "Análise semântica aceita laços com break/continue dentro de if/else");

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(allGotosDefined(inst), "Todo goto aponta para um rótulo definido no fluxo if/else");

        printSeparator();
    }

    /**
     * TESTE 17: While Aninhado em 3 Níveis com Break no Laço do Meio (Caso de Borda 2)
     */
    public static void runTest17() {
        printHeader("TESTE 17: While Aninhado em 3 Níveis com Break no Laço Médio (Caso de Borda 2)");
        String code = """
        int a = 0;
        int b = 0;
        int c = 0;
        while (a < 2) {
            a = a + 1;
            b = 0;
            while (b < 3) {
                b = b + 1;
                c = 0;
                while (c < 4) {
                    c = c + 1;
                    print(c);
                }
                if (b == 2) {
                    break;
                }
            }
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        check(errors.isEmpty(), "Análise semântica correta para 3 níveis de laços aninhados");

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(allGotosDefined(inst), "Todo goto aponta para rótulo definido em 3 níveis");
        // Laço 1: L1..L2, Laço 2: L3..L4, Laço 3: L5..L6. Break no laço 2 salta para L4.
        check(inst.contains("goto L4"), "Break no laço médio salta estritamente para o fim do laço médio (L4)");

        printSeparator();
    }

    /**
     * TESTE 18: Continue como Último Comando do Corpo do While (Caso de Borda 3)
     */
    public static void runTest18() {
        printHeader("TESTE 18: Continue como Último Comando do Corpo do While (Caso de Borda 3)");
        String code = """
        int i = 0;
        while (i < 5) {
            i = i + 1;
            print(i);
            continue;
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        check(errors.isEmpty(), "Análise semântica válida para continue no fim do bloco");

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(allGotosDefined(inst), "Todo goto aponta para rótulo definido");
        check(inst.contains("goto L1"), "Continue emite retorno explícito para L_start");

        printSeparator();
    }

    /**
     * TESTE 19: Break em While cuja Condição usa Constant Folding (Caso de Borda 4)
     */
    public static void runTest19() {
        printHeader("TESTE 19: Break em While com Condição Otimizada por Constant Folding (Caso de Borda 4)");
        String code = """
        int i = 0;
        while (i < (2 + 3)) {
            i = i + 1;
            if (i == 3) {
                break;
            }
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TACTranslator tac = new TACTranslator();
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        check(inst.contains("t1 = i < 5"), "Condição com expressão '2 + 3' dobrada para '5' pelo Constant Folding");
        check(inst.contains("goto L2"), "Break emite salto correto para L_end (L2)");

        printSeparator();
    }

    /**
     * TESTE 20: Expressões com Várias Temporárias antes de Break (Caso de Borda 5)
     */
    public static void runTest20() {
        printHeader("TESTE 20: Expressões com Várias Temporárias antes do Break (Caso de Borda 5)");
        String code = """
        int x = 0;
        while (x < 10) {
            int a = (x + 1) * (x + 2);
            int b = (a + 3) * (a + 4);
            x = x + 1;
            if (b > 100) {
                break;
            }
            print(b);
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        TempPool pool = new TempPool();
        TACTranslator tac = new TACTranslator(pool, true, true);
        List<String> inst = tac.generate(ast);
        System.out.println("\n--- CÓDIGO DE TRÊS ENDEREÇOS (TAC) ---");
        tac.printInstructions();

        System.out.println("\n" + pool.getSummaryReport());

        check(allGotosDefined(inst), "Todo goto aponta para rótulo definido");
        check(pool.getTotalReuses() > 0, "TempPool efetuou reuso de temporárias antes do desvio");
        check(inst.contains("goto L2"), "Break emite desvio limpo para L2");

        printSeparator();
    }

    /**
     * TESTE 21: Break Fora de Laço dentro de Bloco Else (Caso de Borda 6)
     */
    public static void runTest21() {
        printHeader("TESTE 21: Break Fora de Laço dentro de Bloco Else (Caso de Borda 6)");
        String code = """
        int x = 10;
        if (x > 5) {
            print(x);
        } else {
            break;
        }
        """;
        System.out.println("Código MicroC de Entrada:\n" + code);

        ProgramNode ast = parse(code);

        SemanticAnalyzer sa = new SemanticAnalyzer();
        List<SemanticError> errors = sa.analyze(ast);
        System.out.println("Erros semânticos detectados:");
        for (SemanticError err : errors) {
            System.out.println("  " + err);
        }

        boolean semOk = !errors.isEmpty() && errors.stream().anyMatch(e -> e.rule.equals("R8-BreakForaDeLaco") && e.line == 5);
        check(semOk, "SemanticAnalyzer detecta R8-BreakForaDeLaco dentro do else na linha 5");

        boolean tacHandled = false;
        try {
            TACTranslator tac = new TACTranslator();
            tac.generate(ast);
        } catch (RuntimeException e) {
            tacHandled = e.getMessage().contains("break fora de laço");
            System.out.println("Exceção capturada no TACTranslator: " + e.getMessage());
        }
        check(tacHandled, "TACTranslator impede geração de TAC para break dentro de else fora de laço");

        printSeparator();
    }

    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================

    public static ProgramNode parse(String code) {
        Lexer lexer = new Lexer(code);
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = lexer.nextToken();
            tokens.add(t);
        } while (t.type != TokenType.EOF);

        Parser parser = new Parser(tokens);
        return parser.parseProgram();
    }

    private static void printHeader(String title) {
        System.out.println("\n================================================================================");
        System.out.println("  " + title);
        System.out.println("================================================================================");
    }

    private static void printSeparator() {
        System.out.println("--------------------------------------------------------------------------------");
    }
}
