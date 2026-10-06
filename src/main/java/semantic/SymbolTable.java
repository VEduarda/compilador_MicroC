package semantic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Tabela de Símbolos com suporte a escopos aninhados.
 *
 * Implementada como uma PILHA de escopos: cada bloco { }
 * (corpo de if, while, ou o programa principal) empurra um
 * novo mapa de símbolos na pilha; ao sair do bloco, o mapa
 * é removido.
 *
 * Isso é o que permite implementar corretamente:
 *   - Escopo global x escopo local (Exercício 2 do PDF)
 *   - Sombreamento / shadowing (Exercício 6 do PDF)
 *   - Detecção de redeclaração no MESMO escopo (Exercício 8)
 *
 * Além da pilha "ativa" (usada durante a análise), mantemos um
 * histórico com TODOS os símbolos já declarados, na ordem em
 * que apareceram, para poder imprimir a Tabela de Símbolos
 * completa ao final (Exercício 1 e 10 do PDF).
 */
public class SymbolTable {

    private final Deque<Map<String, Symbol>> scopes =
            new ArrayDeque<>();

    private final List<Symbol> history =
            new ArrayList<>();

    public SymbolTable() {
        // Escopo global (nível 0)
        enterScope();
    }

    /* Abre um novo escopo (entra em um bloco { }). */
    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    /* Fecha o escopo atual (sai de um bloco { }). */
    public void exitScope() {
        scopes.pop();
    }

    /* Nível do escopo atual: 0 = global, 1 = primeiro bloco, ... */
    public int currentScopeLevel() {
        return scopes.size() - 1;
    }

    /*
     * Tenta declarar um novo símbolo no escopo ATUAL.
     *
     * Retorna false se já existir um identificador com o mesmo
     * nome nesse mesmo escopo (erro de redeclaração). Declarar
     * um nome já usado em um escopo MAIS EXTERNO é permitido
     * (isso é shadowing, não é erro).
     */
    public boolean declare(Symbol symbol) {

        Map<String, Symbol> current =
                scopes.peek();

        if (current.containsKey(symbol.name)) {
            return false;
        }

        current.put(symbol.name, symbol);
        history.add(symbol);
        return true;
    }

    /*
     * Verifica se o nome já existe especificamente no escopo
     * atual (usado para checar redeclaração).
     */
    public boolean declaredInCurrentScope(String name) {
        return scopes.peek().containsKey(name);
    }

    /*
     * Busca um símbolo percorrendo do escopo mais interno para
     * o mais externo (regra padrão de resolução de nomes).
     * Retorna null se o identificador não foi declarado em
     * nenhum escopo visível.
     */
    public Symbol lookup(String name) {

        for (Map<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        return null;
    }

    /* Todos os símbolos declarados durante toda a análise. */
    public List<Symbol> getHistory() {
        return history;
    }

    /* Verifica se um identificador é uma constante conhecida. */
    public boolean isConstant(String name) {
        Symbol sym = lookup(name);
        return sym != null && sym.isConstant();
    }

    /* Retorna o valor constante de um símbolo, se houver. */
    public String getConstantValue(String name) {
        Symbol sym = lookup(name);
        return (sym != null) ? sym.constantValue : null;
    }

    /* Define ou atualiza o valor constante de um símbolo. */
    public void setConstantValue(String name, String value) {
        Symbol sym = lookup(name);
        if (sym != null) {
            sym.constantValue = value;
        }
    }
}
