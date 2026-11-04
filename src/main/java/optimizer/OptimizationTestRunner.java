package optimizer;

import backend.CompilerPipeline;
import backend.CompilerPipeline.CompilationResult;
import backend.ExecutionResult;
import backend.TACInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Bateria de Testes e Demonstrações da Otimização (Item 5) e Execução Back-End
 * (Item 4).
 */
public class OptimizationTestRunner {

    private static int totalChecks = 0;
    private static int failedChecks = 0;

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            boolean success = runTests();
            if (!success || failedChecks > 0) {
                System.err.printf("%n[FALHA] %d de %d verificações falharam na bateria de Otimização e Execução.%n",
                        failedChecks, totalChecks);
                System.exit(1);
            }
        } catch (Throwable t) {
            System.err.println("\n[ERRO CRÍTICO DE EXECUÇÃO]: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
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

    public static boolean runTests() {
        totalChecks = 0;
        failedChecks = 0;

        System.out.println("================================================================================");
        System.out.println("       BATERIA DE TESTES DE OTIMIZAÇÃO (ITEM 5) E EXECUÇÃO (ITEM 4)             ");
        System.out.println("================================================================================\n");

        // ---------------------------------------------------------------------
        // TESTE 1: Exemplo Exato da Seção 5 do PDF
        // Antes:
        // t1 = 10 * 2
        // t2 = t1 + 5
        // t3 = t2
        // x = t3
        // Depois:
        // x = 25
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("--- TESTE 1: Exemplo Exato da Seção 5 do PDF (Dobramento, Propagação e DCE) ---");
        List<String> rawPdfExample = List.of(
                "t1 = 10 * 2",
                "t2 = t1 + 5",
                "t3 = t2",
                "x = t3");
        TACOptimizer opt1 = new TACOptimizer();
        List<String> optPdfResult = opt1.optimize(rawPdfExample);
=======
        try {
            System.out.println("--- TESTE 1: Exemplo Exato da Seção 5 do PDF (Dobramento, Propagação e DCE) ---");
            List<String> rawPdfExample = List.of(
                    "t1 = 10 * 2",
                    "t2 = t1 + 5",
                    "t3 = t2",
                    "x = t3"
            );
            TACOptimizer opt1 = new TACOptimizer();
            List<String> optPdfResult = opt1.optimize(rawPdfExample);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(optPdfResult.size() == 1, "Exemplo do PDF reduzido a exatamente 1 instrução");
            check(optPdfResult.contains("x = 25"), "Instrução final gerada é exatamente 'x = 25'");

            // Execução no interpretador
            TACInterpreter interp = new TACInterpreter(false);
            ExecutionResult resRaw = interp.execute(rawPdfExample);
            ExecutionResult resOpt = interp.execute(optPdfResult);

<<<<<<< HEAD
        check(resRaw.getVariable("x").equals(25), "Execução do TAC original resulta em x = 25");
        check(resOpt.getVariable("x").equals(25), "Execução do TAC otimizado resulta em x = 25");
        check(resRaw.getVariable("x").equals(resOpt.getVariable("x")),
                "Equivalência observável preservada estritamente!");
=======
            check(resRaw.getVariable("x").equals(25), "Execução do TAC original resulta em x = 25");
            check(resOpt.getVariable("x").equals(25), "Execução do TAC otimizado resulta em x = 25");
            check(resRaw.getVariable("x").equals(resOpt.getVariable("x")), "Equivalência observável preservada estritamente!");
        } catch (Throwable t) {
            check(false, "Teste 1 abortado por exceção: " + t.getMessage());
        }
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

        // ---------------------------------------------------------------------
        // TESTE 2: Simplificações Algébricas
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 2: Simplificações Algébricas (Identidades Clássicas) ---");
        List<String> rawAlg = List.of(
                "t1 = a + 0",
                "t2 = 0 + b",
                "t3 = c * 1",
                "t4 = d * 0",
                "t5 = e - 0",
                "t6 = f / 1");
        TACOptimizer opt2 = new TACOptimizer();
        List<String> optAlg = opt2.optimize(rawAlg);
=======
        try {
            System.out.println("\n--- TESTE 2: Simplificações Algébricas (Identidades Clássicas) ---");
            List<String> rawAlg = List.of(
                    "t1 = a + 0",
                    "t2 = 0 + b",
                    "t3 = c * 1",
                    "t4 = d * 0",
                    "t5 = e - 0",
                    "t6 = f / 1"
            );
            TACOptimizer opt2 = new TACOptimizer();
            List<String> optAlg = opt2.optimize(rawAlg);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(optAlg.contains("t1 = a"), "Identidade aditiva: a + 0 -> a");
            check(optAlg.contains("t2 = b"), "Identidade aditiva: 0 + b -> b");
            check(optAlg.contains("t3 = c"), "Identidade multiplicativa: c * 1 -> c");
            check(optAlg.contains("t4 = 0"), "Anulação multiplicativa: d * 0 -> 0");
            check(optAlg.contains("t5 = e"), "Identidade subtrativa: e - 0 -> e");
            check(optAlg.contains("t6 = f"), "Divisão neutra: f / 1 -> f");
        } catch (Throwable t) {
            check(false, "Teste 2 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 3: Eliminação de Subexpressões Comuns (CSE)
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 3: Eliminação de Subexpressões Comuns (CSE) ---");
        List<String> rawCSE = List.of(
                "t1 = a + b",
                "x = t1",
                "t2 = a + b",
                "y = t2");
        TACOptimizer opt3 = new TACOptimizer();
        List<String> optCSE = opt3.optimize(rawCSE);

        // t2 = a + b deve ser eliminado, reutilizando t1 / x
        boolean hasDuplicateAdd = false;
        int countAdd = 0;
        for (String s : optCSE) {
            if (s.contains("a + b"))
                countAdd++;
=======
        try {
            System.out.println("\n--- TESTE 3: Eliminação de Subexpressões Comuns (CSE) ---");
            List<String> rawCSE = List.of(
                    "t1 = a + b",
                    "x = t1",
                    "t2 = a + b",
                    "y = t2"
            );
            TACOptimizer opt3 = new TACOptimizer();
            List<String> optCSE = opt3.optimize(rawCSE);

            // t2 = a + b deve ser eliminado, reutilizando t1 / x
            boolean hasDuplicateAdd = false;
            int countAdd = 0;
            for (String s : optCSE) {
                if (s.contains("a + b")) countAdd++;
            }
            check(countAdd == 1, "Apenas 1 cálculo de 'a + b' é mantido (reúso via CSE)");
        } catch (Throwable t) {
            check(false, "Teste 3 abortado por exceção: " + t.getMessage());
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c
        }

        // ---------------------------------------------------------------------
        // TESTE 4: Eliminação de Código Morto (DCE) e Saltos Redundantes
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 4: Código Morto, Saltos Redundantes e Inalcançáveis ---");
        List<String> rawDCE = List.of(
                "t1 = 10 + 20",
                "t2 = 50",
                "x = 100",
                "goto L1",
                "t3 = 999",
                "L1:",
                "goto L2",
                "L2:",
                "print x");
        TACOptimizer opt4 = new TACOptimizer();
        List<String> optDCE = opt4.optimize(rawDCE);

        check(!optDCE.contains("t3 = 999"), "Instrução inalcançável após goto eliminada");
        check(!optDCE.contains("goto L2"), "Salto redundante imediatamente anterior a L2: eliminado");
        check(!optDCE.contains("t1 = 30") && !optDCE.contains("t1 = 10 + 20"), "Temporária t1 não lida eliminada");
        check(!optDCE.contains("t2 = 50"), "Temporária t2 não lida eliminada");
        check(optDCE.contains("print 100") || (optDCE.contains("x = 100") && optDCE.contains("print x")),
                "Instrução útil print mantida");
=======
        try {
            System.out.println("\n--- TESTE 4: Código Morto, Saltos Redundantes e Inalcançáveis ---");
            List<String> rawDCE = List.of(
                    "t1 = 10 + 20",
                    "t2 = 50",
                    "x = 100",
                    "goto L1",
                    "t3 = 999",
                    "L1:",
                    "goto L2",
                    "L2:",
                    "print x"
            );
            TACOptimizer opt4 = new TACOptimizer();
            List<String> optDCE = opt4.optimize(rawDCE);

            check(!optDCE.contains("t3 = 999"), "Instrução inalcançável após goto eliminada");
            check(!optDCE.contains("goto L2"), "Salto redundante imediatamente anterior a L2: eliminado");
            check(!optDCE.contains("t1 = 30") && !optDCE.contains("t1 = 10 + 20"), "Temporária t1 não lida eliminada");
            check(!optDCE.contains("t2 = 50"), "Temporária t2 não lida eliminada");
            check(optDCE.contains("print 100") || (optDCE.contains("x = 100") && optDCE.contains("print x")), 
                    "Instrução útil print mantida");
        } catch (Throwable t) {
            check(false, "Teste 4 abortado por exceção: " + t.getMessage());
        }
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

        // ---------------------------------------------------------------------
        // TESTE 5: Pipeline Completo End-to-End (Fonte MicroC -> Execução)
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 5: Pipeline Completo End-to-End (MicroC -> Execução) ---");
        String prog1 = """
                int a = 10;
                int b = 20;
                int c = (a * 2) + (b / 2);
                print(c);
                """;
        CompilerPipeline pipeline = new CompilerPipeline();
        CompilationResult res1 = pipeline.compileString(prog1);
=======
        try {
            System.out.println("\n--- TESTE 5: Pipeline Completo End-to-End (MicroC -> Execução) ---");
            String prog1 = """
            int a = 10;
            int b = 20;
            int c = (a * 2) + (b / 2);
            print(c);
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult res1 = pipeline.compileString(prog1);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(res1.success, "Pipeline compilou com sucesso");
            check(res1.executionResult != null, "Back-end executou com sucesso");
            check(res1.executionResult.getOutputLines().contains("30"), "Resultado impresso correto: 30 (10*2 + 20/2)");
        } catch (Throwable t) {
            check(false, "Teste 5 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 6: Programa com Laço While e Execução
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 6: Laço While com Acumulador e Execução no Back-end ---");
        String progWhile = """
                int sum = 0;
                int i = 1;
                while (i <= 5) {
                    sum = sum + i;
                    i = i + 1;
                }
                print(sum);
                """;
        CompilationResult resWhile = pipeline.compileString(progWhile);
=======
        try {
            System.out.println("\n--- TESTE 6: Laço While com Acumulador e Execução no Back-end ---");
            String progWhile = """
            int sum = 0;
            int i = 1;
            while (i <= 5) {
                sum = sum + i;
                i = i + 1;
            }
            print(sum);
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult resWhile = pipeline.compileString(progWhile);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(resWhile.success, "Compilação de laço while concluída com sucesso");
            check(resWhile.executionResult.getOutputLines().contains("15"), "Somatório de 1 a 5 calculado: 15");
            check(resWhile.executionResult.getVariable("sum").equals(15), "Variável 'sum' na memória final tem valor 15");
        } catch (Throwable t) {
            check(false, "Teste 6 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 7: Exemplo da Seção 8 do PDF (Função main com retorno)
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 7: Exemplo da Seção 8 do PDF (int main() { int a; a = 10; return a; }) ---");
        String progMain = """
                int main() {
                    int a;
                    a = 10;
                    return a;
                }
                """;
        CompilationResult resMain = pipeline.compileString(progMain);
=======
        try {
            System.out.println("\n--- TESTE 7: Exemplo da Seção 8 do PDF (int main() { int a; a = 10; return a; }) ---");
            String progMain = """
            int main() {
                int a;
                a = 10;
                return a;
            }
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult resMain = pipeline.compileString(progMain);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(resMain.success, "Função main do PDF Seção 8 compilada com sucesso");
            check(resMain.executionResult.getReturnValue() != null && resMain.executionResult.getReturnValue() == 10,
                    "Retorno do main capturado corretamente: 10");
        } catch (Throwable t) {
            check(false, "Teste 7 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 8: Detecção e Tratamento de Erros Semânticos (Item 2)
        // ---------------------------------------------------------------------
<<<<<<< HEAD
        System.out.println("\n--- TESTE 8: Tratamento de Erro Semântico (Variável Não Declarada) ---");
        String progErro = """
                int x = 10;
                y = 20;
                """;
        CompilationResult resErro = pipeline.compileString(progErro);
=======
        try {
            System.out.println("\n--- TESTE 8: Tratamento de Erro Semântico (Variável Não Declarada) ---");
            String progErro = """
            int x = 10;
            y = 20;
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult resErro = pipeline.compileString(progErro);
>>>>>>> 0386036b6b61a970d21b8675db2f510d7ba7fe3c

            check(!resErro.success, "Pipeline abortou compilação para variável não declarada");
            check(resErro.failedStage.equals("Análise Semântica"), "Etapa com falha identificada: Análise Semântica");
            check(resErro.semanticErrors.size() >= 1, "Pelo menos 1 erro semântico registrado");
        } catch (Throwable t) {
            check(false, "Teste 8 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 9: Preservação de Escopos e Identidade de Variáveis (Item 6)
        // ---------------------------------------------------------------------
        try {
            System.out.println("\n--- TESTE 9: Preservação de Escopos e Identidade de Variáveis (Item 6) ---");
            String progShadow = """
            int x = 10;
            {
                int x = 20;
                print(x);
            }
            print(x);
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult resShadow = pipeline.compileString(progShadow);

            check(resShadow.success, "Pipeline compilou programa com sombreamento de variáveis");
            check(resShadow.executionResult != null, "Back-end executou programa com escopos distintos");
            check(resShadow.executionResult.getOutputLines().equals(List.of("20", "10")),
                    "Saída observável preservada: 20 (bloco interno) seguido de 10 (bloco externo)");
            check(resShadow.executionResult.getVariable("x").equals(10),
                    "Memória do interpretador preserva escopo 0 (x = 10)");
            check(resShadow.executionResult.getVariable("x", 1).equals(20),
                    "Memória do interpretador preserva escopo interno 1 (x_1 = 20)");
            check(resShadow.rawTAC.stream().anyMatch(s -> s.startsWith("x_1 = 20")),
                    "TAC preserva identidade distinta da variável interna (x_1)");
        } catch (Throwable t) {
            check(false, "Teste 9 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // TESTE 10: Preservação de Escopos em Blocos Irmãos e Aninhados (Item 6)
        // ---------------------------------------------------------------------
        try {
            System.out.println("\n--- TESTE 10: Preservação de Escopos em Blocos Irmãos e Aninhados (Item 6) ---");
            String progSibling = """
            int a = 1;
            {
                int a = 2;
                print(a);
            }
            {
                int a = 3;
                print(a);
            }
            print(a);
            """;
            CompilerPipeline pipeline = new CompilerPipeline();
            CompilationResult resSibling = pipeline.compileString(progSibling);
            check(resSibling.success, "Blocos irmãos com mesmo identificador compilados com sucesso");
            check(resSibling.executionResult.getOutputLines().equals(List.of("2", "3", "1")),
                    "Saídas dos blocos irmãos e externo preservadas (2, 3, 1)");
            check(resSibling.executionResult.getVariable("a").equals(1), "Escopo externo tem a = 1");
            check(resSibling.executionResult.getVariable("a_1") != null && resSibling.executionResult.getVariable("a_1").equals(2),
                    "Primeiro bloco irmão tem identidade a_1 = 2");
            check(resSibling.executionResult.getVariable("a_2") != null && resSibling.executionResult.getVariable("a_2").equals(3),
                    "Segundo bloco irmão tem identidade a_2 = 3");
        } catch (Throwable t) {
            check(false, "Teste 10 abortado por exceção: " + t.getMessage());
        }

        // ---------------------------------------------------------------------
        // Resumo final
        // ---------------------------------------------------------------------
        System.out.println("\n================================================================================");
        System.out.printf("   TOTAL DE CHECKS DE OTIMIZAÇÃO E EXECUÇÃO: %d (FALHAS: %d)%n", totalChecks, failedChecks);
        System.out.println("================================================================================\n");

        return failedChecks == 0;
    }
}
