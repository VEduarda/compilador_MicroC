package ast;

import ast.Nodes.*;

/**
 * Interface do padrão de projeto Visitor para percurso desacoplado da AST do
 * MicroC.
 *
 * Permite implementar múltiplos percursos e tradutores independentes
 * (RPN, TAC, Pretty-Printer, etc.) sem modificar as classes concretas dos nós.
 *
 * Princípio: "Uma AST, Múltiplas Traduções".
 */
public interface ASTVisitor {

    void visit(ProgramNode node);

    void visit(DeclarationNode node);

    void visit(AssignmentNode node);

    void visit(PrintNode node);

    void visit(BlockNode node);

    void visit(IfNode node);

    void visit(WhileNode node);

    void visit(BreakNode node);

    void visit(ContinueNode node);

    void visit(BinOpNode node);

    void visit(NumberNode node);

    void visit(IdentifierNode node);

    void visit(StringNode node);

    default void visit(UnaryOpNode node) {
    }

    default void visit(ReturnNode node) {
    }

    default void visit(FunctionNode node) {
    }

    default void visit(FunctionCallNode node) {
    }

    /**
     * Ponto de entrada utilitário para despacho polimórfico a partir de nó
     * genérico.
     */
    default void visit(Node node) {
        if (node != null) {
            node.accept(this);
        }
    }
}
