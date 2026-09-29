package lexer;

/*
 * Representa um token produzido pelo scanner lexico.
 *
 * Um token e a menor unidade significativa do codigo fonte.
 * Cada token guarda tres informacoes:
 *   - type:   a categoria do token (numero, identificador, operador...)
 *   - lexeme: o texto exato como aparece no codigo fonte
 *   - line:   a linha onde o token foi encontrado
 *
 * A informacao de linha e util para exibir mensagens de erro
 * indicando onde o problema ocorreu no programa.
 */
public class Token {

    public final TokenType type;
    public final String lexeme;
    public final int line;

    /*
     * Cria um novo token com tipo, lexema e numero de linha.
     */
    public Token(TokenType type, String lexeme, int line) {
        this.type   = type;
        this.lexeme = lexeme;
        this.line   = line;
    }

    /*
     * Retorna uma representacao textual do token
     * com colunas alinhadas para facilitar a leitura
     * na tabela de tokens exibida no console.
     */
    @Override
    public String toString() {
        return String.format("%-12s -> %-20s (linha %d)", type, lexeme, line);
    }
}
