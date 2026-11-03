package lexer;

/**
 * Representa um erro léxico identificado durante a fase de análise léxica (Scanner).
 *
 * Indica a linha do código-fonte onde ocorreu o caractere ou sequência não reconhecida.
 */
public class LexicalError extends RuntimeException {

    public final int line;
    public final String detail;

    public LexicalError(int line, String detail) {
        super(String.format("[ERRO LÉXICO] Linha %d: %s", line, detail));
        this.line = line;
        this.detail = detail;
    }
}
