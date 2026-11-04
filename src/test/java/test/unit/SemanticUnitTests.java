package test.unit;

import ast.Nodes.ProgramNode;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;
import test.common.TestFramework;

import java.util.ArrayList;
import java.util.List;

public final class SemanticUnitTests {

    private SemanticUnitTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Semântica identifica variável não declarada", () -> {
            List<SemanticError> errors = analyze("print(unknown);");
            TestFramework.assertTrue(hasRule(errors, "R1-NaoDeclarada"), "Esperava regra de variável não declarada");
        });

        tests.add("Semântica identifica declaração duplicada no mesmo escopo", () -> {
            List<SemanticError> errors = analyze("int x; int x;");
            TestFramework.assertTrue(hasRule(errors, "R2-Redeclaracao"), "Esperava regra de redeclaração");
        });

        tests.add("Semântica aceita tipos compatíveis e promoção int para float", () -> {
            List<SemanticError> errors = analyze("int count = 2; float value = count + 1.5;");
            TestFramework.assertEquals(0, errors.size(), "Programa compatível não deve ter erros");
        });

        tests.add("Semântica identifica atribuição incompatível", () -> {
            List<SemanticError> errors = analyze("int count; count = \"text\";");
            TestFramework.assertTrue(hasRule(errors, "R3-TipoIncompativel"), "Esperava incompatibilidade de tipos");
        });

        tests.add("Semântica identifica operação aritmética incompatível", () -> {
            List<SemanticError> errors = analyze("int count = 1 + \"text\";");
            TestFramework.assertTrue(hasRule(errors, "R4-OperadorInvalidoParaTipo"),
                    "Esperava operação inválida para string");
        });

        tests.add("Semântica exige inicialização e impede reatribuição de constantes", () -> {
            List<SemanticError> reassignmentErrors = analyze("const int fixed = 1; fixed = 2;");
            TestFramework.assertTrue(hasRule(reassignmentErrors, "R9-ReatribuicaoConstante"),
                    "Esperava erro ao reatribuir uma constante");

            List<SemanticError> initializationErrors = analyze("const float rate;");
            TestFramework.assertTrue(hasRule(initializationErrors, "R9-ConstanteNaoInicializada"),
                    "Esperava erro para constante sem inicialização");
        });
    }

    public static List<SemanticError> analyze(String source) {
        ProgramNode program = new Parser(tokens(source)).parseProgram();
        return new SemanticAnalyzer().analyze(program);
    }

    private static boolean hasRule(List<SemanticError> errors, String rule) {
        return errors.stream().anyMatch(error -> error.rule.equals(rule));
    }

    private static List<Token> tokens(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = lexer.nextToken();
            tokens.add(token);
        } while (token.type != TokenType.EOF);
        return tokens;
    }
}
