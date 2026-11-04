package semantic;

import java.util.ArrayList;
import java.util.List;

/*
 * Representa uma entrada da Tabela de Símbolos.
 *
 * Cada símbolo guarda as informações que a análise semântica
 * precisa consultar sobre um identificador (variável):
 *
 *   name       -> nome do identificador (ex.: "x")
 *   type       -> tipo declarado (ex.: "int")
 *   category   -> categoria do identificador (ex.: VARIABLE)
 *   scopeLevel -> nível de escopo em que foi declarado
 *                 (0 = escopo global, 1 = dentro do primeiro
 *                 bloco { }, 2 = bloco aninhado, etc.)
 *   line       -> linha do código-fonte onde foi declarado
 *   initialized-> indica se já recebeu um valor (declaração
 *                 com inicializador ou atribuição posterior)
 */
public class Symbol {

    /*
     * Categoria do identificador dentro da Tabela de Símbolos.
     * O enum já deixa o código preparado para, no futuro,
     * acrescentar FUNCTION e PARAMETER (ver README do projeto).
     */
    public enum Category {
        VARIABLE,
        PARAMETER,
        FUNCTION,
        CONSTANT
    }

    public final String name;
    public final String type;
    public final Category category;
    public final int scopeLevel;
    public final int line;
    public final boolean isConst;

    public boolean initialized;
    public String constantValue;
    public List<String> paramTypes = new ArrayList<>();

    public Symbol(
            String name,
            String type,
            Category category,
            int scopeLevel,
            int line,
            boolean initialized) {

        this(name, type, category, scopeLevel, line, initialized, null, false);
    }

    public Symbol(
            String name,
            String type,
            Category category,
            int scopeLevel,
            int line,
            boolean initialized,
            String constantValue) {

        this(name, type, category, scopeLevel, line, initialized, constantValue, false);
    }

    public Symbol(
            String name,
            String type,
            Category category,
            int scopeLevel,
            int line,
            boolean initialized,
            String constantValue,
            boolean isConst) {

        this.name = name;
        this.type = type;
        this.category = category;
        this.scopeLevel = scopeLevel;
        this.line = line;
        this.initialized = initialized;
        this.constantValue = constantValue;
        this.isConst = isConst;
    }

    public boolean isConstant() {
        return isConst || category == Category.CONSTANT;
    }

    @Override
    public String toString() {
        String constInfo = (constantValue != null) ? " (const=" + constantValue + ")" : "";
        return String.format(
                "%-10s | %-6s | %-9s | escopo %-2d | linha %-3d | %s%s",
                name,
                type,
                category,
                scopeLevel,
                line,
                initialized ? "inicializada" : "não inicializada",
                constInfo);
    }
}
