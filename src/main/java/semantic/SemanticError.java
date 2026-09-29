package semantic;

/*
 * Representa um erro semântico encontrado durante a análise.
 *
 * O PDF de exercícios pede, para cada erro, que se indique a
 * linha e a regra violada — por isso guardamos os dois campos
 * separadamente, além da mensagem descritiva.
 */
public class SemanticError {

    public final int line;
    public final String rule;
    public final String message;

    public SemanticError(int line, String rule, String message) {
        this.line = line;
        this.rule = rule;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format(
                "[ERRO SEMÂNTICO] Linha %d (%s): %s",
                line,
                rule,
                message);
    }
}
