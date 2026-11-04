package test.unit;

import lexer.Lexer;
import lexer.LexicalError;
import lexer.Token;
import lexer.TokenType;
import test.common.TestFramework;

import java.util.ArrayList;
import java.util.List;

public final class LexerUnitTests {

    private LexerUnitTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Lexer reconhece identificadores", () -> {
            List<Token> tokens = scan("alpha _value value2");
            TestFramework.assertEquals(
                    List.of(TokenType.ID, TokenType.ID, TokenType.ID, TokenType.EOF),
                    types(tokens), "Tipos dos identificadores");
            TestFramework.assertEquals("_value", tokens.get(1).lexeme, "Lexema do identificador");
        });

        tests.add("Lexer reconhece palavras reservadas", () -> {
            List<Token> tokens = scan("int float void if else while break continue return print");
            TestFramework.assertEquals(
                    List.of(TokenType.INT, TokenType.FLOAT, TokenType.VOID, TokenType.IF, TokenType.ELSE,
                            TokenType.WHILE, TokenType.BREAK, TokenType.CONTINUE, TokenType.RETURN,
                            TokenType.PRINT, TokenType.EOF),
                    types(tokens), "Tipos das palavras reservadas");
        });

        tests.add("Lexer reconhece operadores e delimitadores", () -> {
            List<Token> tokens = scan("= == != && || ! + - * / < > <= >= ( ) { } , ;");
            TestFramework.assertEquals(
                    List.of(TokenType.ASSIGN, TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.AND, TokenType.OR,
                            TokenType.NOT, TokenType.PLUS, TokenType.MINUS, TokenType.MULT, TokenType.DIV,
                            TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE, TokenType.LPAREN,
                            TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE, TokenType.COMMA,
                            TokenType.SEMICOLON, TokenType.EOF),
                    types(tokens), "Tipos dos operadores e delimitadores");
        });

        tests.add("Lexer reconhece números inteiros e decimais", () -> {
            List<Token> tokens = scan("42 3.125");
            TestFramework.assertEquals(TokenType.NUMBER, tokens.get(0).type, "Tipo do inteiro");
            TestFramework.assertEquals("42", tokens.get(0).lexeme, "Lexema do inteiro");
            TestFramework.assertEquals(TokenType.NUMBER, tokens.get(1).type, "Tipo do decimal");
            TestFramework.assertEquals("3.125", tokens.get(1).lexeme, "Lexema do decimal");
        });

        tests.add("Lexer rejeita caractere inválido", () -> {
            Lexer lexer = new Lexer("@");
            LexicalError error = TestFramework.assertThrows(
                    LexicalError.class, lexer::nextToken, "Esperava erro léxico");
            TestFramework.assertTrue(error.detail.contains("@"), "Erro deve identificar o caractere");
        });

        tests.add("Lexer rejeita literal decimal incompleto", () -> {
            Lexer lexer = new Lexer("12.");
            TestFramework.assertThrows(
                    LexicalError.class, lexer::nextToken, "Esperava erro para decimal sem dígitos fracionários");
        });
    }

    public static List<Token> scan(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = lexer.nextToken();
            tokens.add(token);
        } while (token.type != TokenType.EOF);
        return tokens;
    }

    private static List<TokenType> types(List<Token> tokens) {
        return tokens.stream().map(token -> token.type).toList();
    }
}
