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

        if (match(TokenType.INT, TokenType.FLOAT, TokenType.VOID))
            return parseDeclarationOrFunction();

        if (match(TokenType.PRINT))
            return parsePrint();

        if (match(TokenType.IF))
            return parseIf();

        if (match(TokenType.WHILE))
            return parseWhile();

        if (match(TokenType.BREAK))
            return parseBreak();

        if (match(TokenType.CONTINUE))
            return parseContinue();

        if (match(TokenType.RETURN))
            return parseReturn();

        if (check(TokenType.ID))
            return parseAssignment();

        throw error(
                "Comando inválido");
    }

    /*
     * Analisa uma declaração de variável ou definição de função.
     *
     * Exemplos:
     * int x;
     * int x = 10;
     * int main() { ... }
     * int soma(int a, int b) { ... }
     */
    private Node parseDeclarationOrFunction() {
        Token typeTok = previous();
        Token id = consume(TokenType.ID);

        // Verifica se é uma definição de função (ex.: int main() { ... })
        if (match(TokenType.LPAREN)) {
            List<DeclarationNode> params = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    Token paramType = consume(TokenType.INT, TokenType.FLOAT);
                    Token paramId = consume(TokenType.ID);
                    DeclarationNode decl = new DeclarationNode(paramType.lexeme, paramId.lexeme, null);
                    decl.at(paramId.line);
                    params.add(decl);
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN);
            BlockNode body = parseBlock();
            return new FunctionNode(typeTok.lexeme, id.lexeme, params, body).at(id.line);
        }

        // Caso contrário, é uma declaração de variável
        Node initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }

        consume(TokenType.SEMICOLON);

        return new DeclarationNode(
                typeTok.lexeme,
                id.lexeme,
                initializer).at(id.line);
    }

    /*
     * Analisa o comando return.
     *
     * Exemplos:
     * return;
     * return a;
     * return a + 10;
     */
    private Node parseReturn() {
        Token retTok = previous();
        Node value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        consume(TokenType.SEMICOLON);
        return new ReturnNode(value).at(retTok.line);
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
     * Analisa o comando break.
     *
     * Exemplo:
     *
     * break;
     */
    private Node parseBreak() {
        Token tok = previous();
        consume(TokenType.SEMICOLON);
        return new BreakNode().at(tok.line);
    }

    /*
     * Analisa o comando continue.
     *
     * Exemplo:
     *
     * continue;
     */
    private Node parseContinue() {
        Token tok = previous();
        consume(TokenType.SEMICOLON);
        return new ContinueNode().at(tok.line);
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
    /*
     * Analisa expressões com hierarquia completa de precedência:
     *   1. Lógica OR (||)
     *   2. Lógica AND (&&)
     *   3. Igualdade (==, !=)
     *   4. Relacional (<, >, <=, >=)
     *   5. Aditiva (+, -)
     *   6. Multiplicativa (*, /)
     *   7. Unária (!, -)
     *   8. Fator / Primário (número, string, id, parênteses)
     */
    private Node parseExpression() {
        return parseLogicalOr();
    }

    private Node parseLogicalOr() {
        Node expr = parseLogicalAnd();
        while (match(TokenType.OR)) {
            Token op = previous();
            Node right = parseLogicalAnd();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseLogicalAnd() {
        Node expr = parseEquality();
        while (match(TokenType.AND)) {
            Token op = previous();
            Node right = parseEquality();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseEquality() {
        Node expr = parseRelational();
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token op = previous();
            Node right = parseRelational();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseRelational() {
        Node expr = parseAdditive();
        while (match(TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE)) {
            Token op = previous();
            Node right = parseAdditive();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseAdditive() {
        Node expr = parseMultiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Node right = parseMultiplicative();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseMultiplicative() {
        Node expr = parseUnary();
        while (match(TokenType.MULT, TokenType.DIV)) {
            Token op = previous();
            Node right = parseUnary();
            expr = new BinOpNode(expr, op.lexeme, right).at(op.line);
        }
        return expr;
    }

    private Node parseUnary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            Token op = previous();
            Node right = parseUnary();
            return new UnaryOpNode(op.lexeme, right).at(op.line);
        }
        return parseFactor();
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

        if (match(TokenType.NUMBER)) {
            Token number = previous();
            // O lexema decimal carrega o tipo até a AST; o semântico usa esse dado na promoção.
            String type = number.lexeme.contains(".") ? "float" : "int";
            return new NumberNode(number.lexeme, type).at(number.line);
        }

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

    private Token consume(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                return advance();
            }
        }
        throw error("Esperado " + String.join(" ou ",
                java.util.Arrays.stream(types).map(Enum::name).toList()));
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
    private SyntacticError error(
            String message) {

        Token t = peek();
        String lex = (t.lexeme == null || t.lexeme.isEmpty()) ? t.type.name() : t.lexeme;
        return new SyntacticError(
                t.line,
                message + ", mas foi encontrado '" + lex + "'");
    }
}