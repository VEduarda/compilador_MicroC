package parser;

import ast.Node;
import ast.Nodes.*;

import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/*
 * Esta classe é responsável por fazer a análise sintática.
 *
 * Ela recebe a lista de tokens produzida pelo Lexer
 * e verifica se o programa segue as regras da linguagem.
 *
 * Ao mesmo tempo, vai montando a AST
 * (Árvore Sintática Abstrata).
 */
public class Parser {

    // Lista de tokens recebidos do analisador léxico
    private final List<Token> tokens;

    // Posição atual dentro da lista de tokens
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*
     * Inicia a análise do programa inteiro.
     *
     * Enquanto não chegar ao final do arquivo,
     * continua lendo comandos e adicionando-os
     * à árvore sintática.
     */
    public ProgramNode parseProgram() {

        List<Node> statements =
                new ArrayList<>();

        while (!isAtEnd()) {

            statements.add(
                    parseStatement());
        }

        return new ProgramNode(statements);
    }

    /*
     * Descobre qual tipo de comando está sendo lido.
     *
     * Dependendo do token encontrado, chama o método
     * responsável por analisar aquele comando.
     */
    private Node parseStatement() {

        if (match(TokenType.INT))
            return parseDeclaration();

        if (match(TokenType.PRINT))
            return parsePrint();

        if (match(TokenType.IF))
            return parseIf();

        if (match(TokenType.WHILE))
            return parseWhile();

        if (check(TokenType.ID))
            return parseAssignment();

        throw error(
                "Comando inválido");
    }

    /*
     * Analisa uma declaração de variável.
     *
     * Exemplos:
     *
     * int x;
     * int x = 10;
     */
    private Node parseDeclaration() {

        Token id =
                consume(TokenType.ID);

        Node initializer = null;

        // Verifica se existe um valor inicial
        if (match(TokenType.ASSIGN)) {

            initializer =
                    parseExpression();
        }

        consume(TokenType.SEMICOLON);

        return new DeclarationNode(
                "int",
                id.lexeme,
                initializer).at(id.line);
    }

    /*
     * Analisa uma atribuição.
     *
     * Exemplo:
     *
     * x = 20;
     */
    private Node parseAssignment() {

        Token id =
                consume(TokenType.ID);

        consume(TokenType.ASSIGN);

        Node value =
                parseExpression();

        consume(TokenType.SEMICOLON);

        return new AssignmentNode(
                id.lexeme,
                value).at(id.line);
    }

    /*
     * Analisa o comando print().
     *
     * Exemplo:
     *
     * print(x);
     */
    private Node parsePrint() {

        consume(TokenType.LPAREN);

        Node expr =
                parseExpression();

        consume(TokenType.RPAREN);
        consume(TokenType.SEMICOLON);

        return new PrintNode(expr).at(expr.line);
    }

    /*
     * Analisa uma estrutura condicional if.
     *
     * Também verifica se existe um bloco else.
     */
    private Node parseIf() {

        consume(TokenType.LPAREN);

        Node condition =
                parseExpression();

        consume(TokenType.RPAREN);

        Node thenBranch =
                parseBlock();

        Node elseBranch = null;

        // Caso exista um else, analisa seu bloco
        if (match(TokenType.ELSE)) {

            elseBranch =
                    parseBlock();
        }

        return new IfNode(
                condition,
                thenBranch,
                elseBranch).at(condition.line);
    }

    /*
     * Analisa uma estrutura de repetição while.
     *
     * Exemplo:
     *
     * while(x > 0){
     *     print(x);
     * }
     */
    private Node parseWhile() {

        consume(TokenType.LPAREN);

        Node condition =
                parseExpression();

        consume(TokenType.RPAREN);

        Node body =
                parseBlock();

        return new WhileNode(
                condition,
                body).at(condition.line);
    }

    /*
     * Analisa um bloco delimitado por chaves.
     *
     * Tudo que estiver entre { e }
     * será armazenado como parte do mesmo bloco.
     */
    private BlockNode parseBlock() {

        Token brace =
                consume(TokenType.LBRACE);

        List<Node> statements =
                new ArrayList<>();

        while (!check(TokenType.RBRACE)
                && !isAtEnd()) {

            statements.add(
                    parseStatement());
        }

        consume(TokenType.RBRACE);

        return (BlockNode) new BlockNode(
                statements).at(brace.line);
    }

    /*
     * Analisa expressões matemáticas e relacionais.
     *
     * Exemplos:
     *
     * x + 10
     * a - b
     * x > 5
     * y == 20
     */
    private Node parseExpression() {

        Node expr =
                parseTerm();

        while (match(
                TokenType.PLUS,
                TokenType.MINUS,
                TokenType.LT,
                TokenType.GT,
                TokenType.LTE,
                TokenType.GTE,
                TokenType.EQUAL,
                TokenType.NOT_EQUAL)) {

            Token op =
                    previous();

            Node right =
                    parseTerm();

            expr = new BinOpNode(
                    expr,
                    op.lexeme,
                    right).at(op.line);
        }

        return expr;
    }

    /*
     * Analisa operações de multiplicação e divisão.
     *
     * Essas operações possuem prioridade maior
     * dentro das expressões.
     */
    private Node parseTerm() {

        Node expr =
                parseFactor();

        while (match(
                TokenType.MULT,
                TokenType.DIV)) {

            Token op =
                    previous();

            Node right =
                    parseFactor();

            expr = new BinOpNode(
                    expr,
                    op.lexeme,
                    right).at(op.line);
        }

        return expr;
    }

    /*
     * Analisa os elementos básicos de uma expressão.
     *
     * Pode ser:
     * - Número
     * - String
     * - Variável
     * - Expressão entre parênteses
     */
    private Node parseFactor() {

        if (match(TokenType.NUMBER))
            return new NumberNode(
                    previous().lexeme).at(previous().line);

        if (match(TokenType.STRING))
            return new StringNode(
                    previous().lexeme).at(previous().line);

        if (match(TokenType.ID))
            return new IdentifierNode(
                    previous().lexeme).at(previous().line);

        if (match(TokenType.LPAREN)) {

            Node expr =
                    parseExpression();

            consume(TokenType.RPAREN);

            return expr;
        }

        throw error(
                "Expressão inválida");
    }

    /*
     * Verifica se o token atual corresponde
     * a algum dos tipos recebidos.
     *
     * Se corresponder, avança para o próximo token.
     */
    private boolean match(TokenType... types) {

        for (TokenType type : types) {

            if (check(type)) {

                advance();
                return true;
            }
        }

        return false;
    }

    /*
     * Garante que o próximo token seja do tipo esperado.
     *
     * Caso contrário, gera um erro sintático.
     */
    private Token consume(TokenType type) {

        if (check(type))
            return advance();

        throw error(
                "Esperado " + type);
    }

    /*
     * Apenas verifica o tipo do token atual,
     * sem avançar na lista.
     */
    private boolean check(TokenType type) {

        if (isAtEnd())
            return false;

        return peek().type == type;
    }

    /*
     * Move o ponteiro para o próximo token.
     */
    private Token advance() {

        if (!isAtEnd())
            current++;

        return previous();
    }

    /*
     * Verifica se chegou ao final do arquivo.
     */
    private boolean isAtEnd() {

        return peek().type ==
                TokenType.EOF;
    }

    /*
     * Retorna o token atual.
     */
    private Token peek() {

        return tokens.get(current);
    }

    /*
     * Retorna o token anteriormente processado.
     */
    private Token previous() {

        return tokens.get(current - 1);
    }

    /*
     * Cria uma mensagem de erro detalhada,
     * informando a linha e o token onde o problema ocorreu.
     */
    private RuntimeException error(
            String message) {

        return new RuntimeException(
                "[ERRO SINTÁTICO] Linha "
                        + peek().line
                        + ": "
                        + message
                        + " encontrado "
                        + peek().lexeme);
    }
}