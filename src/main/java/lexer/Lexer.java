package lexer;

import java.util.HashMap;
import java.util.Map;

/*
 * Scanner lexico do MicroC.
 *
 * Responsavel por ler o codigo fonte caractere por caractere
 * e transformar esse texto em uma sequencia de tokens.
 *
 * O metodo principal e nextToken(), que deve ser chamado
 * em loop ate retornar um token do tipo EOF.
 *
 * Funcionalidades implementadas:
 *   - Numeros inteiros e decimais
 *   - Strings entre aspas duplas
 *   - Identificadores e palavras reservadas
 *   - Operadores aritmeticos: + - * /
 *   - Operadores relacionais: == != < > <= >=
 *   - Operador de atribuicao: =
 *   - Delimitadores: ( ) { } ;
 *   - Comentarios de linha: // ate o fim da linha
 *   - Comentarios de bloco: /* ate asterisco barra
 *   - Rastreamento do numero de linha
 *   - Deteccao e relato de erros lexicos
 */
public class Lexer {

    private final String input;
    private int pos;
    private int line;

    /*
     * Mapa que associa cada palavra reservada ao seu tipo de token.
     * Usado para distinguir palavras reservadas de identificadores comuns.
     */
    private final Map<String, TokenType> keywords;

    /*
     * Mapa que associa cada operador (em texto) ao seu tipo de token.
     * Inclui operadores de um e dois caracteres.
     */
    private final Map<String, TokenType> operators;

    /*
     * Inicializa o lexer com o codigo fonte a ser analisado.
     * A posicao comeca em 0 e a contagem de linhas comeca em 1.
     */
    public Lexer(String input) {
        this.input = input;
        this.pos   = 0;
        this.line  = 1;

        keywords  = new HashMap<>();
        operators = new HashMap<>();

        initializeKeywords();
        initializeOperators();
    }

    /*
     * Preenche o mapa de palavras reservadas.
     * Qualquer identificador presente nesse mapa sera classificado
     * como palavra reservada em vez de ID.
     */
    private void initializeKeywords() {
        keywords.put("int",      TokenType.INT);
        keywords.put("float",    TokenType.FLOAT);
        keywords.put("void",     TokenType.VOID);
        keywords.put("if",       TokenType.IF);
        keywords.put("else",     TokenType.ELSE);
        keywords.put("while",    TokenType.WHILE);
        keywords.put("break",    TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("return",   TokenType.RETURN);
        keywords.put("print",    TokenType.PRINT);
    }

    /*
     * Preenche o mapa de operadores.
     * Os operadores de dois caracteres (como == e >=) sao verificados
     * antes dos de um caractere para evitar conflito.
     */
    private void initializeOperators() {
        operators.put("=",  TokenType.ASSIGN);
        operators.put("==", TokenType.EQUAL);
        operators.put("!=", TokenType.NOT_EQUAL);
        operators.put("+",  TokenType.PLUS);
        operators.put("-",  TokenType.MINUS);
        operators.put("*",  TokenType.MULT);
        operators.put("/",  TokenType.DIV);
        operators.put("<",  TokenType.LT);
        operators.put(">",  TokenType.GT);
        operators.put("<=", TokenType.LTE);
        operators.put(">=", TokenType.GTE);
        operators.put("&&", TokenType.AND);
        operators.put("||", TokenType.OR);
        operators.put("!", TokenType.NOT);
    }

    /*
     * Retorna o caractere atual sem avançar a posicao.
     * Retorna o caractere nulo '\0' se chegou ao fim do texto.
     */
    private char peek() {
        if (pos >= input.length()) return '\0';
        return input.charAt(pos);
    }

    /*
     * Retorna o proximo caractere (um a frente do atual)
     * sem avancar a posicao. Util para reconhecer operadores
     * de dois caracteres como == e <=.
     */
    private char peekNext() {
        if (pos + 1 >= input.length()) return '\0';
        return input.charAt(pos + 1);
    }

    /*
     * Retorna o caractere atual e avanca a posicao em um.
     * E o metodo basico de consumo de caracteres.
     */
    private char next() {
        char current = peek();
        if (current != '\0') pos++;
        return current;
    }

    /*
     * Avanca enquanto o caractere atual for espaco, tabulacao
     * ou quebra de linha. Incrementa o contador de linhas
     * sempre que uma quebra de linha e encontrada.
     */
    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            if (peek() == '\n') line++;
            next();
        }
    }

    /*
     * Ignora comentarios de linha e de bloco.
     *
     * Comentario de linha: comeca com // e vai ate o fim da linha.
     * Comentario de bloco: comeca com /* e termina com asterisco barra.
     *   Se o bloco nao for fechado, lanca um erro lexico.
     */
    private void skipComment() {

        // Comentario de linha
        if (peek() == '/' && peekNext() == '/') {
            while (peek() != '\n' && peek() != '\0') next();

        // Comentario de bloco
        } else if (peek() == '/' && peekNext() == '*') {
            next(); next(); // consome /*

            while (!(peek() == '*' && peekNext() == '/')) {
                if (peek() == '\0')
                    throw new LexicalError(line, "comentário de bloco não fechado");
                if (peek() == '\n') line++;
                next();
            }
            next(); next(); // consome */
        }
    }

    /*
     * Lê um literal inteiro ou decimal no formato dígitos.dígitos.
     */
    private Token scanNumber() {
        StringBuilder buffer = new StringBuilder();
        while (Character.isDigit(peek())) buffer.append(next());

        if (peek() == '.') {
            buffer.append(next());
            if (!Character.isDigit(peek())) {
                throw new LexicalError(line, "literal decimal deve ter dígitos após o ponto");
            }
            while (Character.isDigit(peek())) buffer.append(next());
        }

        return new Token(TokenType.NUMBER, buffer.toString(), line);
    }

    /*
     * Le um identificador ou palavra reservada.
     * Aceita letras, digitos e underscore.
     * Apos ler o texto completo, verifica se e uma palavra reservada.
     * Se for, retorna o tipo correspondente; caso contrario, retorna ID.
     */
    private Token scanIdentifier() {
        StringBuilder buffer = new StringBuilder();
        while (Character.isLetterOrDigit(peek()) || peek() == '_')
            buffer.append(next());

        String lexeme = buffer.toString();
        if (keywords.containsKey(lexeme))
            return new Token(keywords.get(lexeme), lexeme, line);

        return new Token(TokenType.ID, lexeme, line);
    }

    /*
     * Le uma string delimitada por aspas duplas.
     * O conteudo entre as aspas e armazenado como lexema.
     * Se a string nao for fechada antes do fim do arquivo,
     * um erro lexico e lancado.
     */
    private Token scanString() {
        StringBuilder buffer = new StringBuilder();
        next(); // consome a aspa de abertura

        while (peek() != '"' && peek() != '\0') {
            if (peek() == '\n') line++;
            buffer.append(next());
        }

        if (peek() == '\0')
            throw new LexicalError(line, "string literal não fechada antes do fim do arquivo");

        next(); // consome a aspa de fechamento
        return new Token(TokenType.STRING, buffer.toString(), line);
    }

    /*
     * Tenta reconhecer um operador na posicao atual.
     * Verifica primeiro se ha um operador de dois caracteres.
     * Se nao houver, tenta um operador de um caractere.
     * Retorna null se nenhum operador for reconhecido.
     */
    private Token scanOperator() {
        String twoChars = "" + peek() + peekNext();
        if (operators.containsKey(twoChars)) {
            next(); next();
            return new Token(operators.get(twoChars), twoChars, line);
        }

        String oneChar = "" + peek();
        if (operators.containsKey(oneChar)) {
            next();
            return new Token(operators.get(oneChar), oneChar, line);
        }

        return null;
    }

    /*
     * Retorna o proximo token do codigo fonte.
     *
     * O metodo primeiro ignora espacos e comentarios,
     * depois tenta reconhecer o proximo token na seguinte ordem:
     *   1. Fim de arquivo
     *   2. String
     *   3. Numero
     *   4. Identificador ou palavra reservada
     *   5. Operador
     *   6. Delimitador
     *
     * Se nenhum padrao for reconhecido, lanca um erro lexico
     * indicando o caractere invalido e a linha onde ocorreu.
     */
    public Token nextToken() {

        // Ignora espacos em branco e comentarios antes do proximo token
        while (true) {
            skipWhitespace();
            if (peek() == '/' && (peekNext() == '/' || peekNext() == '*')) {
                skipComment();
                continue;
            }
            break;
        }

        // Fim do arquivo
        if (peek() == '\0')
            return new Token(TokenType.EOF, "", line);

        // String
        if (peek() == '"')
            return scanString();

        // Numero
        if (Character.isDigit(peek()))
            return scanNumber();

        // Identificador ou palavra reservada
        if (Character.isLetter(peek()) || peek() == '_')
            return scanIdentifier();

        // Operador
        Token opToken = scanOperator();
        if (opToken != null) return opToken;

        // Delimitadores
        char c = next();
        switch (c) {
            case '(': return new Token(TokenType.LPAREN,    "(", line);
            case ')': return new Token(TokenType.RPAREN,    ")", line);
            case '{': return new Token(TokenType.LBRACE,    "{", line);
            case '}': return new Token(TokenType.RBRACE,    "}", line);
            case ',': return new Token(TokenType.COMMA,     ",", line);
            case ';': return new Token(TokenType.SEMICOLON, ";", line);
            default:
                throw new LexicalError(line, "caractere inválido ou não reconhecido: '" + c + "'");
        }
    }
}
