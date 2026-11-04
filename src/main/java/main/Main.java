package main;

import backend.CompilerPipeline;
import backend.CompilerPipeline.CompilationResult;
import backend.ExecutionResult;
import backend.TACInterpreter;
import lexer.Token;
import optimizer.OptimizationReport;
import optimizer.OptimizationReport.Transformation;
import optimizer.OptimizationTestRunner;
import optimizer.TACOptimizer;
import sdt.SDTTestRunner;
import semantic.SemanticError;
import semantic.Symbol;

import java.util.List;

/**
 * Ponto de Entrada Principal do Compilador MicroC.
 *
 * Todos os programas de exemplo estão embutidos diretamente no código como texto (Strings multiline),
 * sem dependência de arquivos externos no disco.
 *
 * Cobertura completa dos itens solicitados do PDF:
 *   - Item 2: Descrição do Projeto (Léxico, Sintático, Semântico, TAC e Back-end)
 *   - Item 3: Fluxo Esperado do Compilador (Pipeline de 7 etapas)
 *   - Item 4: Resultado Final Esperado (Execução do programa no Back-end via interpretador TAC)
 *   - Item 5: Otimização de Código (Folding, Propagação, CSE, Simplificação e DCE)
 *   - Tratamento de Erros: Erros Léxicos, Sintáticos e Semânticos identificados com localização e descrição clara.
 */
public class Main {

    // =========================================================================
    // PROGRAMAS DE EXEMPLO EMBUTIDOS COMO TEXTO (CÓDIGO-FONTE MICROC)
    // =========================================================================

    /** Exemplo 1: Programa completo com expressões, condicionais e laço while */
    public static final String PROGRAMA_1_COMPLETO = """
    int x = 2 + 3 * 4;
    int y = (x + 5) * 2;
    int sum = 0;
    int i = 0;
    while (i < 3) {
        sum = sum + i;
        i = i + 1;
    }
    if (y > 20) {
        print(y);
    } else {
        print(0);
    }
    print(sum);
    """;

    /** Exemplo 2: Laço de repetição com acumulador somatório */
    public static final String PROGRAMA_2_WHILE = """
    int i = 0;
    int sum = 0;
    while (i < 5) {
        i = i + 1;
        sum = sum + i;
        print(i);
    }
    print(sum);
    """;

    /** Exemplo 3: Código do exemplo da Seção 5 do PDF (Otimização) */
    public static final String PROGRAMA_3_PDF_SECAO5 = """
    int t1;
    int t2;
    int t3;
    int x;
    t1 = 10 * 2;
    t2 = t1 + 5;
    t3 = t2;
    x = t3;
    print(x);
    """;

    /** Exemplo 4: Código da Seção 8 do PDF (Função int main() com return) */
    public static final String PROGRAMA_4_MAIN_SECAO8 = """
    int main() {
        int a;
        a = 10;
        return a;
    }
    """;

    /** Exemplo 5: Código para teste de tratamento de ERRO SEMÂNTICO */
    public static final String PROGRAMA_ERRO_SEMANTICO = """
    int a = 10;
    int b = "texto incompativel";
    c = 30;
    """;

    /** Exemplo 6: Código para teste de tratamento de ERRO SINTÁTICO */
    public static final String PROGRAMA_ERRO_SINTATICO = """
    int x = ;
    """;

    /** Exemplo 7: Código para teste de tratamento de ERRO LÉXICO */
    public static final String PROGRAMA_ERRO_LEXICO = """
    int x = 10 @ 5;
    """;

    // =========================================================================
    // PONTO DE ENTRADA MAIN
    // =========================================================================

    public static void main(String[] args) {
        // Força saída padrão e de erros em UTF-8 para exibição correta de caracteres acentuados
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));

        System.out.println("================================================================================");
        System.out.println("            COMPILADOR MICROC - DEMONSTRAÇÃO COMPLETA INTEGRADA                 ");
        System.out.println("              Itens Desenvolvidos: 2 (Projeto), 3 (Fluxo), 4 (Execução) e 5 (Otimização) ");
        System.out.println("================================================================================\n");

        boolean allPassed = true;

        try {
            // ---------------------------------------------------------------------
            // 1. DEMONSTRAÇÃO DO PIPELINE COMPLETO (PROGRAMA 1)
            // ---------------------------------------------------------------------
            System.out.println(">>> CENARIO 1: PROGRAMA COMPLETO MICROC (EXECUCAO DE TODAS AS ETAPAS)");
            if (!runFullPipeline(PROGRAMA_1_COMPLETO, "Exemplo 1: Programa Integrado MicroC")) {
                allPassed = false;
            }

            // ---------------------------------------------------------------------
            // 2. DEMONSTRAÇÃO DA SEÇÃO 5 DO PDF (OTIMIZAÇÃO ANTES E DEPOIS)
            // ---------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println(">>> CENARIO 2: EXEMPLO EXATO DA SECAO 5 DO ENUNCIADO .PDF (OTIMIZACAO)");
            System.out.println("================================================================================");
            if (!demonstratePdfSection5()) {
                allPassed = false;
            }

            // ---------------------------------------------------------------------
            // 3. DEMONSTRAÇÃO DO LAÇO WHILE COM ACUMULADOR (PROGRAMA 2)
            // ---------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println(">>> CENARIO 3: LACO WHILE COM ACUMULADOR E PRINTS");
            System.out.println("================================================================================");
            if (!runFullPipeline(PROGRAMA_2_WHILE, "Exemplo 2: Laço While com Acumulador")) {
                allPassed = false;
            }

            // ---------------------------------------------------------------------
            // 4. DEMONSTRAÇÃO DA SEÇÃO 8 DO PDF (FUNÇÃO MAIN COM RETORNO)
            // ---------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println(">>> CENARIO 4: EXEMPLO DA SECAO 8 DO ENUNCIADO .PDF (int main() COM RETURN)");
            System.out.println("================================================================================");
            if (!runFullPipeline(PROGRAMA_4_MAIN_SECAO8, "Exemplo 4: Seção 8 - int main() com return")) {
                allPassed = false;
            }

            // ---------------------------------------------------------------------
            // 5. DEMONSTRAÇÃO DETALHADA DO TRATAMENTO DE ERROS (LÉXICOS, SINTÁTICOS E SEMÂNTICOS)
            // ---------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println(">>> CENARIO 5: TRATAMENTO DE ERROS (LEXICOS, SINTATICOS E SEMANTICOS)");
            System.out.println("================================================================================");
            if (!demonstrateErrorHandling()) {
                allPassed = false;
            }

            // ---------------------------------------------------------------------
            // 6. EXECUÇÃO DAS BATERIAS DE TESTES AUTOMATIZADOS (TODOS COM SUCESSO)
            // ---------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println(">>> CENÁRIO 6: BATERIA DE TESTES DE VALIDAÇÃO (OTIMIZACAO, EXECUCAO E SDT)");
            System.out.println("================================================================================\n");
            boolean optSuccess = OptimizationTestRunner.runTests();
            if (!optSuccess || OptimizationTestRunner.getFailedChecks() > 0) {
                allPassed = false;
            }

            boolean sdtSuccess = SDTTestRunner.runAllTests();
            if (!sdtSuccess || SDTTestRunner.getFailedChecks() > 0) {
                allPassed = false;
            }

        } catch (Throwable t) {
            System.err.println("\n[ERRO CRÍTICO DE EXECUÇÃO]: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        if (allPassed) {
            System.out.println("\n================================================================================");
            System.out.println("       TODAS AS ETAPAS E TRATAMENTOS DE ERRO VALIDADOS COM 100% DE SUCESSO!      ");
            System.out.println("================================================================================\n");
        } else {
            System.err.println("\n================================================================================");
            System.err.println("       FALHA DETECTADA NO PIPELINE OU NAS BATERIAS DE TESTES!                   ");
            if (OptimizationTestRunner.getFailedChecks() > 0) {
                System.err.printf("       -> Bateria de Otimização e Execução: %d falha(s)%n", OptimizationTestRunner.getFailedChecks());
            }
            if (SDTTestRunner.getFailedChecks() > 0) {
                System.err.printf("       -> Bateria de Tradução Dirigida por Sintaxe (SDT): %d falha(s)%n", SDTTestRunner.getFailedChecks());
            }
            System.err.println("================================================================================\n");
            System.exit(1);
        }
    }

    /**
     * Executa o pipeline de compilação completo para um código-fonte MicroC em texto.
     */
    public static boolean runFullPipeline(String code, String programTitle) {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("FLUXO DE COMPILACAO CANONICO: " + programTitle);
        System.out.println("--------------------------------------------------------------------------------");

        System.out.println("\n>>> 0. CODIGO-FONTE MICROC:");
        System.out.println(code.trim());

        CompilerPipeline pipeline = new CompilerPipeline();
        CompilationResult res = pipeline.compileString(code, programTitle);

        // ETAPA 1: ANÁLISE LÉXICA
        System.out.println("\n--- 1. ANALISE LEXICA (" + res.tokens.size() + " tokens em " + String.format("%.4f", res.lexTimeMs) + " ms) ---");
        for (Token t : res.tokens) {
            System.out.println("   " + t);
        }

        if (!res.success && "Análise Léxica".equals(res.failedStage)) {
            System.err.println("\n[COMPILACAO INTERROMPIDA POR ERRO LEXICO]:");
            System.err.println("   " + res.errorMessage);
            return false;
        }

        // ETAPA 2: ANÁLISE SINTÁTICA
        System.out.println("\n--- 2. ANALISE SINTATICA - AST GERADA (Tempo: " + String.format("%.4f", res.parseTimeMs) + " ms) ---");
        if (res.ast != null) {
            res.ast.print("   ");
        }

        if (!res.success && "Análise Sintática".equals(res.failedStage)) {
            System.err.println("\n[COMPILACAO INTERROMPIDA POR ERRO SINTATICO]:");
            System.err.println("   " + res.errorMessage);
            return false;
        }

        // ETAPA 3: ANÁLISE SEMÂNTICA
        System.out.println("\n--- 3. ANALISE SEMANTICA & TABELA DE SIMBOLOS (Tempo: " + String.format("%.4f", res.semanticTimeMs) + " ms) ---");
        if (res.symbolTable != null) {
            System.out.println("   NOME         | TIPO     | CATEGORIA   | ESCOPO    | LINHA    | STATUS");
            for (Symbol s : res.symbolTable.getHistory()) {
                System.out.println("   " + s);
            }
        }

        if (!res.semanticErrors.isEmpty() || (!res.success && "Análise Semântica".equals(res.failedStage))) {
            System.err.println("\n[COMPILACAO INTERROMPIDA POR ERROS SEMANTICOS (" + res.semanticErrors.size() + ")]: ");
            for (SemanticError err : res.semanticErrors) {
                System.err.println("   " + err);
            }
            return false;
        } else {
            System.out.println("\n   >> Análise semântica concluída sem nenhum erro!");
        }

        // ETAPA 4: CÓDIGO INTERMEDIÁRIO (TAC / IR)
        System.out.println("\n--- 4. CODIGO INTERMEDIARIO GERADO (TAC / IR) (Tempo: " + String.format("%.4f", res.tacTimeMs) + " ms) ---");
        for (String inst : res.rawTAC) {
            System.out.println("   " + inst);
        }

        // ETAPA 5: OTIMIZAÇÃO DE CÓDIGO INTERMEDIÁRIO
        if (res.optimizationReport != null) {
            res.optimizationReport.printReport();
        }

        // ETAPAS 6 & 7: GERAÇÃO DE CÓDIGO / BACK-END E EXECUÇÃO
        if (res.executionResult != null) {
            res.executionResult.printSummary();
        }

        if (!res.success) {
            System.err.println("\n[COMPILACAO/EXECUCAO INTERROMPIDA]: " + res.errorMessage);
            return false;
        }

        // VALIDAÇÃO DA PRESERVAÇÃO SEMÂNTICA (EXECUÇÃO COMPARATIVA ORIGINAL VS OTIMIZADO)
        System.out.println("--- VALIDACAO DE PRESERVACAO SEMANTICA (ITEM 5 DO ENUNCIADO) ---");
        TACInterpreter quietInterp = new TACInterpreter(false);
        ExecutionResult execRaw = quietInterp.execute(res.rawTAC);
        ExecutionResult execOpt = quietInterp.execute(res.optimizedTAC);

        boolean outputMatch = execRaw.getOutput().equals(execOpt.getOutput());
        System.out.println("Saída do TAC Original  : " + (execRaw.getOutput().isEmpty() ? "(vazio)" : execRaw.getOutput()));
        System.out.println("Saída do TAC Otimizado : " + (execOpt.getOutput().isEmpty() ? "(vazio)" : execOpt.getOutput()));
        System.out.println("Comportamento Idêntico : " + (outputMatch ? "[SIM - Semântica Preservada]" : "[NÃO - Divergência Detectada]"));
        System.out.printf("Instruções Executadas  : Original = %d ciclos | Otimizado = %d ciclos (Economia de %.1f%%)%n%n",
                execRaw.getInstructionsExecuted(), execOpt.getInstructionsExecuted(),
                execRaw.getInstructionsExecuted() > 0 
                        ? ((execRaw.getInstructionsExecuted() - execOpt.getInstructionsExecuted()) * 100.0 / execRaw.getInstructionsExecuted())
                        : 0.0);

        if (!outputMatch) {
            System.err.println("\n[FALHA DE PRESERVAÇÃO SEMÂNTICA]: Divergência observável entre TAC original e otimizado!");
            return false;
        }
        return true;
    }

    /**
     * Demonstra a otimização conforme o exemplo literal da Seção 5 do PDF.
     */
    private static boolean demonstratePdfSection5() {
        List<String> rawPdfExample = List.of(
                "t1 = 10 * 2",
                "t2 = t1 + 5",
                "t3 = t2",
                "x = t3"
        );

        System.out.println("\nCódigo intermediário antes da otimização (conforme o PDF):");
        for (String s : rawPdfExample) {
            System.out.println("   " + s);
        }

        TACOptimizer pdfOpt = new TACOptimizer();
        List<String> optPdf = pdfOpt.optimize(rawPdfExample);

        System.out.println("\nCódigo intermediário após a otimização:");
        for (String s : optPdf) {
            System.out.println("   " + s);
        }

        System.out.println("\nTransformações realizadas passo a passo:");
        for (Transformation t : pdfOpt.getReport().getTransformations()) {
            System.out.println(t);
        }

        // Execução no Back-end via interpretador TAC
        TACInterpreter interp = new TACInterpreter(false);
        ExecutionResult rawEx = interp.execute(rawPdfExample);
        ExecutionResult optEx = interp.execute(optPdf);

        boolean success = rawEx.getVariable("x") != null
                && rawEx.getVariable("x").equals(optEx.getVariable("x"))
                && Integer.valueOf(25).equals(optEx.getVariable("x"));

        System.out.println("\nComprovacao de execucao no Back-end:");
        System.out.println("   Valor de x (Original)  = " + rawEx.getVariable("x"));
        System.out.println("   Valor de x (Otimizado) = " + optEx.getVariable("x"));
        System.out.println("   Resultado              = " + (success
                ? "SUCESSO: x = 25 (Preservacao semantica e reducao para 1 instrução)"
                : "FALHA: Divergência"));
        return success;
    }

    /**
     * Demonstra detalhadamente o tratamento de erros léxicos, sintáticos e semânticos.
     */
    private static boolean demonstrateErrorHandling() {
        CompilerPipeline pipeline = new CompilerPipeline();
        boolean allPassed = true;

        // 1. ERRO LÉXICO
        System.out.println("\n--- CASO 1: TRATAMENTO DE ERRO LEXICO ---");
        System.out.println("Codigo-fonte analisado:\n" + PROGRAMA_ERRO_LEXICO.trim());
        CompilationResult resLex = pipeline.compileString(PROGRAMA_ERRO_LEXICO, "Teste Erro Lexico");
        System.out.println("Status da Compilacao  : " + (resLex.success ? "SUCESSO" : "INTERROMPIDA COM SEGURANCA"));
        System.out.println("Etapa que detectou    : " + resLex.failedStage);
        System.out.println("Mensagem detalhada    : " + resLex.errorMessage);
        if (resLex.success || !"Análise Léxica".equals(resLex.failedStage)) {
            System.err.println("   [FALHA] Erro léxico não foi detectado ou tratado adequadamente!");
            allPassed = false;
        }

        // 2. ERRO SINTÁTICO
        System.out.println("\n--- CASO 2: TRATAMENTO DE ERRO SINTATICO ---");
        System.out.println("Codigo-fonte analisado:\n" + PROGRAMA_ERRO_SINTATICO.trim());
        CompilationResult resSint = pipeline.compileString(PROGRAMA_ERRO_SINTATICO, "Teste Erro Sintatico");
        System.out.println("Status da Compilacao  : " + (resSint.success ? "SUCESSO" : "INTERROMPIDA COM SEGURANCA"));
        System.out.println("Etapa que detectou    : " + resSint.failedStage);
        System.out.println("Mensagem detalhada    : " + resSint.errorMessage);
        if (resSint.success || !"Análise Sintática".equals(resSint.failedStage)) {
            System.err.println("   [FALHA] Erro sintático não foi detectado ou tratado adequadamente!");
            allPassed = false;
        }

        // 3. ERRO SEMÂNTICO
        System.out.println("\n--- CASO 3: TRATAMENTO DE ERROS SEMANTICOS ---");
        System.out.println("Codigo-fonte analisado:\n" + PROGRAMA_ERRO_SEMANTICO.trim());
        CompilationResult resSem = pipeline.compileString(PROGRAMA_ERRO_SEMANTICO, "Teste Erro Semantico");
        System.out.println("Status da Compilacao  : " + (resSem.success ? "SUCESSO" : "INTERROMPIDA COM SEGURANCA"));
        System.out.println("Etapa que detectou    : " + resSem.failedStage);
        System.out.println("Total de erros achados: " + resSem.semanticErrors.size());
        for (SemanticError e : resSem.semanticErrors) {
            System.out.println("   " + e);
        }
        if (resSem.success || !"Análise Semântica".equals(resSem.failedStage) || resSem.semanticErrors.isEmpty()) {
            System.err.println("   [FALHA] Erro semântico não foi detectado ou tratado adequadamente!");
            allPassed = false;
        }

        return allPassed;
    }
}
