package test.integration;

import ast.Nodes.DeclarationNode;
import ast.Nodes.ProgramNode;
import backend.CompilerPipeline;
import backend.CompilerPipeline.CompilationResult;
import lexer.LexicalError;
import parser.SyntacticError;
import test.common.TestFramework;
import test.unit.AstParserUnitTests;
import test.unit.LexerUnitTests;

public final class CompilerPipelineIntegrationTests {

    private CompilerPipelineIntegrationTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Integração de código-fonte, Lexer, Parser e AST", () -> {
            String source = "int answer = 6 * 7; print(answer);";
            ProgramNode ast = AstParserUnitTests.parse(source);
            TestFramework.assertEquals(2, ast.statements.size(), "Comandos produzidos pela análise");
            TestFramework.assertTrue(ast.statements.get(0) instanceof DeclarationNode,
                    "AST deve começar com uma declaração");
            TestFramework.assertEquals(lexer.TokenType.INT, LexerUnitTests.scan(source).get(0).type,
                    "Lexer deve produzir o token inicial");
        });

        tests.add("Pipeline interrompe código-fonte com erro léxico", () -> {
            CompilationResult result = new CompilerPipeline().compileString("@");
            TestFramework.assertEquals("Análise Léxica", result.failedStage, "Etapa do erro léxico");
            TestFramework.assertTrue(result.errorMessage != null, "Mensagem do erro léxico");
            TestFramework.assertThrows(LexicalError.class, () -> new lexer.Lexer("@").nextToken(),
                    "Lexer deve preservar o tipo de erro");
        });

        tests.add("Pipeline interrompe código-fonte com erro sintático", () -> {
            CompilationResult result = new CompilerPipeline().compileString("int value = ;");
            TestFramework.assertEquals("Análise Sintática", result.failedStage, "Etapa do erro sintático");
            TestFramework.assertThrows(SyntacticError.class,
                    () -> AstParserUnitTests.parse("int value = ;"), "Parser deve preservar o tipo de erro");
        });

        tests.add("Pipeline reporta erro semântico em programa completo", () -> {
            CompilationResult result = new CompilerPipeline().compileString("int value = \"invalid\";");
            TestFramework.assertEquals("Análise Semântica", result.failedStage, "Etapa do erro semântico");
            TestFramework.assertEquals(1, result.semanticErrors.size(), "Quantidade de erros semânticos");
        });

        tests.add("Pipeline produz TAC bruto e otimizado para programa válido", () -> {
            CompilationResult result = new CompilerPipeline().compileString(
                    "int first = 2; int second = 3; int total = first + second; print(total);");
            TestFramework.assertTrue(result.success, "Programa deve compilar com sucesso: " + result.errorMessage);
            TestFramework.assertTrue(!result.rawTAC.isEmpty(), "Deve existir TAC bruto");
            TestFramework.assertTrue(!result.optimizedTAC.isEmpty(), "Deve existir TAC otimizado");
            TestFramework.assertTrue(result.optimizationReport != null, "Deve existir relatório de otimização");
        });

        tests.add("Pipeline executa operações aritméticas e produz o resultado", () -> {
            CompilationResult result = new CompilerPipeline().compileString(
                    "int value = (2 + 3) * 4; print(value);");
            TestFramework.assertTrue(result.success, "Programa deve compilar com sucesso: " + result.errorMessage);
            TestFramework.assertEquals("20", result.executionResult.getOutput(), "Resultado da execução");
        });

        tests.add("Pipeline executa condicionais e laços", () -> {
            String source = """
                    int i = 0;
                    int total = 0;
                    while (i < 4) {
                        if (i < 2) {
                            total = total + i;
                        } else {
                            total = total + 2;
                        }
                        i = i + 1;
                    }
                    print(total);
                    """;
            CompilationResult result = new CompilerPipeline().compileString(source);
            TestFramework.assertTrue(result.success, "Programa deve compilar com sucesso: " + result.errorMessage);
            TestFramework.assertEquals("5", result.executionResult.getOutput(), "Resultado do laço");
        });

        tests.add("Pipeline otimiza e executa preservando o resultado", () -> {
            CompilationResult result = new CompilerPipeline().compileString(
                    "int value = 4; int total = value + 0; print(total);");
            TestFramework.assertTrue(result.success, "Programa deve compilar com sucesso: " + result.errorMessage);
            TestFramework.assertEquals("4", result.executionResult.getOutput(), "Resultado após otimização");
            TestFramework.assertTrue(!result.optimizationReport.getTransformations().isEmpty(),
                    "O relatório deve registrar otimizações realizadas");
        });

        tests.pending("Pipeline compila e executa chamadas de função",
                "O Parser não possui sintaxe de chamada de função; apenas as declarações são reconhecidas.");
    }
}
