package parser;

/**
 * Representa um erro sintático identificado durante a fase de análise sintática (Parser).
 *
 * Indica a linha do código-fonte onde a gramática foi violada e qual token era esperado.
 */
public class SyntacticError extends RuntimeException {

    public final int line;
    public final String tokenFound;
    public final String expected;

    public SyntacticError(int line, String expected, String tokenFound) {
        super(String.format("[ERRO SINTÁTICO] Linha %d: esperado %s, mas foi encontrado '%s'", line, expected, tokenFound));
        this.line = line;
        this.expected = expected;
        this.tokenFound = tokenFound;
    }

    public SyntacticError(int line, String message) {
        super(String.format("[ERRO SINTÁTICO] Linha %d: %s", line, message));
        this.line = line;
        this.expected = "";
        this.tokenFound = "";
    }
}
