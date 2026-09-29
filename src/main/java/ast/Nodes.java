package ast;

import java.util.List;

/*
 * Classe que contém todos os tipos de nós da AST
 * (Árvore Sintática Abstrata).
 *
 * Cada nó representa uma construção da linguagem:
 * programa, variáveis, expressões, comandos, etc.
 */
public class Nodes {

    /*
     * Nó raiz do programa.
     * Armazena todos os comandos encontrados.
     */
    public static class ProgramNode extends Node {

        public final List<Node> statements;

        public ProgramNode(List<Node> statements) {
            this.statements = statements;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "Program");

            for (Node stmt : statements) {
                stmt.print(indent + "  ");
            }
        }
    }

    /*
     * Representa um número literal.
     * Exemplo: 10, 25, 100
     */
    public static class NumberNode extends Node {

        public final String value;

        public NumberNode(String value) {
            this.value = value;
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + "Number(" + value + ")");
        }
    }

    /*
     * Representa um identificador (nome de variável).
     * Exemplo: x, contador, total
     */
    public static class IdentifierNode extends Node {

        public final String name;

        public IdentifierNode(String name) {
            this.name = name;
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + "Identifier(" + name + ")");
        }
    }

    /*
     * Representa uma string literal.
     * Exemplo: "Olá Mundo"
     */
    public static class StringNode extends Node {

        public final String value;

        public StringNode(String value) {
            this.value = value;
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + "String(" + value + ")");
        }
    }

    /*
     * Representa uma operação binária.
     *
     * Exemplos:
     * x + 10
     * a * b
     * x > 5
     */
    public static class BinOpNode extends Node {

        public final Node left;
        public final String op;
        public final Node right;

        public BinOpNode(Node left, String op, Node right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "BinOp(" + op + ")");

            left.print(indent + "  ");
            right.print(indent + "  ");
        }
    }

    /*
     * Representa uma atribuição.
     *
     * Exemplo:
     * x = 20;
     */
    public static class AssignmentNode extends Node {

        public final String name;
        public final Node value;

        public AssignmentNode(String name, Node value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "Assignment(" + name + ")");

            value.print(indent + "  ");
        }
    }

    /*
     * Representa uma declaração de variável.
     *
     * Exemplo:
     * int x = 10;
     */
    public static class DeclarationNode extends Node {

        public final String type;
        public final String name;
        public final Node initializer;

        public DeclarationNode(
                String type,
                String name,
                Node initializer) {

            this.type = type;
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        public void print(String indent) {

            System.out.println(
                    indent + "Declaration(" +
                    type + " " + name + ")");

            if (initializer != null) {
                initializer.print(indent + "  ");
            }
        }
    }

    /*
     * Representa um comando print().
     */
    public static class PrintNode extends Node {

        public final Node expression;

        public PrintNode(Node expression) {
            this.expression = expression;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "Print");

            expression.print(indent + "  ");
        }
    }

    /*
     * Representa um bloco de código delimitado por { }.
     */
    public static class BlockNode extends Node {

        public final List<Node> statements;

        public BlockNode(List<Node> statements) {
            this.statements = statements;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "Block");

            for (Node stmt : statements) {
                stmt.print(indent + "  ");
            }
        }
    }

    /*
     * Representa uma estrutura condicional if/else.
     */
    public static class IfNode extends Node {

        public final Node condition;
        public final Node thenBranch;
        public final Node elseBranch;

        public IfNode(
                Node condition,
                Node thenBranch,
                Node elseBranch) {

            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "If");

            condition.print(indent + "  ");
            thenBranch.print(indent + "  ");

            if (elseBranch != null) {
                elseBranch.print(indent + "  ");
            }
        }
    }

    /*
     * Representa uma estrutura de repetição while.
     */
    public static class WhileNode extends Node {

        public final Node condition;
        public final Node body;

        public WhileNode(Node condition, Node body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public void print(String indent) {

            System.out.println(indent + "While");

            condition.print(indent + "  ");
            body.print(indent + "  ");
        }
    }
}