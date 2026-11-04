package test.unit;

import ast.Node;
import ast.Nodes.AssignmentNode;
import ast.Nodes.BinOpNode;
import ast.Nodes.BlockNode;
import ast.Nodes.DeclarationNode;
import ast.Nodes.IfNode;
import ast.Nodes.NumberNode;
import ast.Nodes.PrintNode;
import ast.Nodes.ProgramNode;
import ast.Nodes.WhileNode;
import lexer.LexicalError;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.Parser;
import parser.SyntacticError;
import test.common.TestFramework;

import java.util.ArrayList;
import java.util.List;

public final class AstParserUnitTests {

    private AstParserUnitTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Parser aceita declarações e expressões válidas", () -> {
            ProgramNode program = parse("int total = 2 + 3 * 4; print(total);");
            TestFramework.assertEquals(2, program.statements.size(), "Quantidade de comandos");
            TestFramework.assertTrue(program.statements.get(0) instanceof DeclarationNode,
                    "Primeiro comando deve ser declaração");
            TestFramework.assertTrue(program.statements.get(1) instanceof PrintNode,
                    "Segundo comando deve ser print");
        });

        tests.add("AST preserva precedência e tipos dos nós", () -> {
            ProgramNode program = parse("int total = 2 + 3 * 4;");
            DeclarationNode declaration = (DeclarationNode) program.statements.get(0);
            BinOpNode addition = (BinOpNode) declaration.initializer;
            TestFramework.assertEquals("+", addition.op, "Operador raiz");
            TestFramework.assertTrue(addition.left instanceof NumberNode, "Operando esquerdo");
            TestFramework.assertTrue(addition.right instanceof BinOpNode, "Multiplicação deve ficar aninhada");
            TestFramework.assertEquals("*", ((BinOpNode) addition.right).op, "Operador de maior precedência");
        });

        tests.add("Parser aceita e constrói nós condicionais e de repetição", () -> {
            ProgramNode program = parse(
                    "int i = 0; if (i < 1) { i = i + 1; } else { print(i); } while (i < 2) { i = i + 1; }");
            TestFramework.assertTrue(program.statements.get(1) instanceof IfNode, "Nó de if");
            IfNode conditional = (IfNode) program.statements.get(1);
            TestFramework.assertTrue(conditional.thenBranch instanceof BlockNode, "Bloco do if");
            TestFramework.assertTrue(conditional.elseBranch instanceof BlockNode, "Bloco do else");
            TestFramework.assertTrue(program.statements.get(2) instanceof WhileNode, "Nó de while");
        });

        tests.add("Parser rejeita sintaxe inválida", () -> {
            List<Token> tokens = tokens("int x = ;");
            TestFramework.assertThrows(
                    SyntacticError.class, () -> new Parser(tokens).parseProgram(),
                    "Esperava erro sintático para expressão ausente");
        });

        tests.add("Parser aceita atribuições com expressões entre parênteses", () -> {
            ProgramNode program = parse("int x; x = (1 + 2) * 3;");
            TestFramework.assertTrue(program.statements.get(1) instanceof AssignmentNode,
                    "Comando deve ser atribuição");
            AssignmentNode assignment = (AssignmentNode) program.statements.get(1);
            TestFramework.assertEquals("x", assignment.name, "Destino da atribuição");
            TestFramework.assertEquals("*", ((BinOpNode) assignment.value).op, "Expressão atribuída");
        });
    }

    public static ProgramNode parse(String source) {
        return new Parser(tokens(source)).parseProgram();
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
