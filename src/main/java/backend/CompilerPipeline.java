package backend;

import ast.Nodes.ProgramNode;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import optimizer.OptimizationReport;
import optimizer.TACOptimizer;
import parser.Parser;
import sdt.TACTranslator;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;
import semantic.SymbolTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline Integrado do Compilador MicroC (Itens 2, 3, 4 e 5 do Enunciado).
 *
 * Implementa o fluxo canônico de compilação:
 *   1. Código-fonte MicroC (via String ou Arquivo)
 *   2. Análise Léxica (Tokens + Detecção de Erros)
 *   3. Análise Sintática (Construção da AST + Detecção de Erros)
 *   4. Análise Semântica (Tabela de Símbolos, Escopos, Tipos + Detecção de Erros)
 *   5. Código Intermediário (Três Endereços - TAC / IR)
 *   6. Otimização de Código (Folding, Propagação, CSE, Simplificações, DCE)
 *   7. Geração de Código / Back-end (Execução no Interpretador de TAC)
 */
public class CompilerPipeline {

    public static class CompilationResult {
        public boolean success = false;
        public String sourceCode;
        public List<Token> tokens = new ArrayList<>();
        public ProgramNode ast;
        public SymbolTable symbolTable;
        public List<SemanticError> semanticErrors = new ArrayList<>();
        public List<String> rawTAC = new ArrayList<>();
        public List<String> optimizedTAC = new ArrayList<>();
        public OptimizationReport optimizationReport;
        public ExecutionResult executionResult;
        public String errorMessage;
        public String failedStage;

        // Métricas de tempo (em milissegundos)
        public double lexTimeMs;
        public double parseTimeMs;
        public double semanticTimeMs;
        public double tacTimeMs;
        public double optTimeMs;
        public double execTimeMs;
    }

    /**
     * Compila e executa o código-fonte contido em um arquivo no caminho especificado.
     */
    public CompilationResult compileFile(String filePath) throws IOException {
        String code = Files.readString(Path.of(filePath));
        return compileString(code, filePath);
    }

    /**
     * Compila e executa uma string de código-fonte MicroC.
     */
    public CompilationResult compileString(String code) {
        return compileString(code, "<memória>");
    }

    /**
     * Compila e executa o código-fonte seguindo estritamente as etapas dos Itens 2, 3, 4 e 5.
     */
    public CompilationResult compileString(String code, String sourceName) {
        CompilationResult res = new CompilationResult();
        res.sourceCode = code;

        try {
            // =================================================================
            // ETAPA 1: ANÁLISE LÉXICA (Item 2 e 3)
            // =================================================================
            long t0 = System.nanoTime();
            Lexer lexer = new Lexer(code);
            Token token;
            do {
                token = lexer.nextToken();
                res.tokens.add(token);
            } while (token.type != TokenType.EOF);
            long t1 = System.nanoTime();
            res.lexTimeMs = (t1 - t0) / 1_000_000.0;

            // =================================================================
            // ETAPA 2: ANÁLISE SINTÁTICA (Item 2 e 3)
            // =================================================================
            long t2 = System.nanoTime();
            Parser parser = new Parser(res.tokens);
            res.ast = parser.parseProgram();
            long t3 = System.nanoTime();
            res.parseTimeMs = (t3 - t2) / 1_000_000.0;

            // =================================================================
            // ETAPA 3: ANÁLISE SEMÂNTICA (Item 2 e 3)
            // =================================================================
            long t4 = System.nanoTime();
            SemanticAnalyzer semantic = new SemanticAnalyzer();
            res.semanticErrors = semantic.analyze(res.ast);
            res.symbolTable = semantic.getSymbolTable();
            long t5 = System.nanoTime();
            res.semanticTimeMs = (t5 - t4) / 1_000_000.0;

            if (!res.semanticErrors.isEmpty()) {
                res.success = false;
                res.failedStage = "Análise Semântica";
                res.errorMessage = res.semanticErrors.size() + " erro(s) semântico(s) detectado(s). A compilação foi interrompida.";
                return res;
            }

            // =================================================================
            // ETAPA 4: GERAÇÃO DE CÓDIGO INTERMEDIÁRIO (TAC / IR) (Item 2 e 3)
            // =================================================================
            long t6 = System.nanoTime();
            // Desativa o folding embutido no visitor para gerar o TAC puro antes da etapa de otimização
            TACTranslator tacTranslator = new TACTranslator(null, true, false, res.symbolTable);
            res.rawTAC = tacTranslator.generate(res.ast);
            long t7 = System.nanoTime();
            res.tacTimeMs = (t7 - t6) / 1_000_000.0;

            // =================================================================
            // ETAPA 5: OTIMIZAÇÃO DE CÓDIGO INTERMEDIÁRIO (Item 5)
            // =================================================================
            long t8 = System.nanoTime();
            TACOptimizer optimizer = new TACOptimizer();
            res.optimizedTAC = optimizer.optimize(res.rawTAC);
            res.optimizationReport = optimizer.getReport();
            long t9 = System.nanoTime();
            res.optTimeMs = (t9 - t8) / 1_000_000.0;

            // =================================================================
            // ETAPAS 6 & 7: GERAÇÃO DE CÓDIGO / BACK-END E EXECUÇÃO (Itens 2, 3 e 4)
            // =================================================================
            long t10 = System.nanoTime();
            TACInterpreter interpreter = new TACInterpreter(false);
            res.executionResult = interpreter.execute(res.optimizedTAC);
            long t11 = System.nanoTime();
            res.execTimeMs = (t11 - t10) / 1_000_000.0;

            res.success = true;

        } catch (lexer.LexicalError e) {
            res.success = false;
            res.failedStage = "Análise Léxica";
            res.errorMessage = e.getMessage();
        } catch (parser.SyntacticError e) {
            res.success = false;
            res.failedStage = "Análise Sintática";
            res.errorMessage = e.getMessage();
        } catch (RuntimeException e) {
            res.success = false;
            res.errorMessage = e.getMessage();
            if (e.getMessage() != null && e.getMessage().contains("LÉXICO")) {
                res.failedStage = "Análise Léxica";
            } else if (e.getMessage() != null && e.getMessage().contains("SINTÁTICO")) {
                res.failedStage = "Análise Sintática";
            } else {
                res.failedStage = "Execução / Back-end";
            }
        }

        return res;
    }
}
