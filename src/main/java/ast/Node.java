package ast;

/**
 * Classe base abstrata para todos os nós da AST (Abstract Syntax Tree).
 *
 * Cada nó implementa o método print() para visualização da árvore
 * com recuo (indentação) proporcional à profundidade.
 */
public abstract class Node {

    /**
     * Linha do código-fonte onde este nó foi originado.
     * Usada pelo Analisador Semântico para reportar erros
     * (ex.: "variável não declarada, linha 7").
     *
     * Valor -1 significa "linha não informada" (nó criado
     * sem passar pelo Parser, ex.: em testes unitários).
     */
    public int line = -1;

    /**
     * Define a linha do nó de forma fluente, permitindo
     * escrever: new NumberNode("10").at(token.line)
     */
    public Node at(int line) {
        this.line = line;
        return this;
    }

    /**
     * Imprime o nó e seus filhos com indentação visual.
     * @param indent prefixo de espaços para o nível atual
     */
    public abstract void print(String indent);

    /** Atalho para imprimir a partir da raiz. */
    public void print() {
        print("");
    }
}