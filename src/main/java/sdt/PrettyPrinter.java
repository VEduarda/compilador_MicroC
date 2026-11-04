package sdt;

import ast.ASTVisitor;
import ast.Node;
import ast.Nodes.*;

/**
 * Tradutor 3: PrettyPrinter (Reimpressor Canônico e Linter).
 *
 * Percorre a AST e reconstrói o código-fonte em linguagem MicroC de forma
 * perfeitamente padronizada (semelhante ao Prettier, Clang-Format ou rustfmt).
 *
 * Características (Seção 3 do Enunciado):
 *   1. Utiliza a variável indentLevel para tabular blocos aninhados com 4 espaços.
 *   2. Insere parênteses canônicos em todas as expressões binárias, explicitando
 *      visualmente a árvore sintática resolvida pelo Parser com respeito às
 *      regras de precedência e associatividade.
 */
public class PrettyPrinter implements ASTVisitor {

    /** Nível atual de indentação (cada nível equivale a 4 espaços). */
    private int indentLevel = 0;

    /** Buffer acumulador do código-fonte canônico formatado. */
    private final StringBuilder output = new StringBuilder();

    /**
     * Formata um nó da AST e retorna a representação em texto padronizada.
     */
    public String format(Node node) {
        output.setLength(0);
        indentLevel = 0;
        if (node != null) {
            node.accept(this);
        }
        return output.toString();
    }

    /**
     * Formata especificamente uma expressão, retornando sua representação canônica
     * com parênteses explícitos.
     */
    public String formatExpression(Node expr) {
        if (expr == null) return "";
        if (expr instanceof BinOpNode b) {
            return "(" + formatExpression(b.left) + " " + b.op + " " + formatExpression(b.right) + ")";
        } else if (expr instanceof UnaryOpNode u) {
            return "(" + u.op + formatExpression(u.expr) + ")";
        } else if (expr instanceof NumberNode n) {
            return n.value;
        } else if (expr instanceof IdentifierNode id) {
            return id.name;
        } else if (expr instanceof StringNode s) {
            return "\"" + s.value + "\"";
        } else if (expr instanceof FunctionCallNode call) {
            StringBuilder sb = new StringBuilder();
            sb.append(call.name).append("(");
            for (int i = 0; i < call.arguments.size(); i++) {
                sb.append(formatExpression(call.arguments.get(i)));
                if (i < call.arguments.size() - 1) sb.append(", ");
            }
            sb.append(")");
            return sb.toString();
        }
        return expr.getClass().getSimpleName();
    }

    public void printFormatted() {
        System.out.println("=== CÓDIGO C CANÔNICO FORMATADO (PRETTY-PRINTER) ===");
        System.out.print(output.toString());
    }

    public String getOutput() {
        return output.toString();
    }

    private String getIndent() {
        return "    ".repeat(Math.max(0, indentLevel));
    }

    // =========================================================================
    // Métodos Visitor Obrigatórios
    // =========================================================================

    @Override
    public void visit(ProgramNode node) {
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(DeclarationNode node) {
        output.append(getIndent());
        if (node.isConst) {
            output.append("const ");
        }
        output.append(node.type).append(" ").append(node.name);
        if (node.initializer != null) {
            output.append(" = ").append(formatExpression(node.initializer));
        }
        output.append(";\n");
    }

    @Override
    public void visit(AssignmentNode node) {
        output.append(getIndent()).append(node.name).append(" = ")
              .append(formatExpression(node.value)).append(";\n");
    }

    @Override
    public void visit(PrintNode node) {
        output.append(getIndent()).append("print(")
              .append(formatExpression(node.expression)).append(");\n");
    }

    @Override
    public void visit(IfNode node) {
        output.append(getIndent()).append("if (").append(formatExpression(node.condition)).append(") {\n");
        indentLevel++;
        formatBranchBody(node.thenBranch);
        indentLevel--;

        if (node.elseBranch != null) {
            output.append(getIndent()).append("} else {\n");
            indentLevel++;
            formatBranchBody(node.elseBranch);
            indentLevel--;
        }
        output.append(getIndent()).append("}\n");
    }

    @Override
    public void visit(WhileNode node) {
        output.append(getIndent()).append("while (").append(formatExpression(node.condition)).append(") {\n");
        indentLevel++;
        formatBranchBody(node.body);
        indentLevel--;
        output.append(getIndent()).append("}\n");
    }

    @Override
    public void visit(BreakNode node) {
        output.append(getIndent()).append("break;\n");
    }

    @Override
    public void visit(ContinueNode node) {
        output.append(getIndent()).append("continue;\n");
    }

    @Override
    public void visit(ReturnNode node) {
        output.append(getIndent()).append("return");
        if (node.value != null) {
            output.append(" ").append(formatExpression(node.value));
        }
        output.append(";\n");
    }

    @Override
    public void visit(FunctionNode node) {
        output.append(getIndent()).append(node.returnType).append(" ").append(node.name).append("(");
        if (node.parameters != null && !node.parameters.isEmpty()) {
            for (int i = 0; i < node.parameters.size(); i++) {
                DeclarationNode p = node.parameters.get(i);
                if (p.isConst) {
                    output.append("const ");
                }
                output.append(p.type).append(" ").append(p.name);
                if (i < node.parameters.size() - 1) {
                    output.append(", ");
                }
            }
        }
        output.append(") {\n");
        indentLevel++;
        formatBranchBody(node.body);
        indentLevel--;
        output.append(getIndent()).append("}\n");
    }

    @Override
    public void visit(FunctionCallNode node) {
        output.append(getIndent()).append(formatExpression(node)).append(";\n");
    }

    @Override
    public void visit(UnaryOpNode node) {
        output.append(formatExpression(node));
    }

    @Override
    public void visit(BlockNode node) {
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(BinOpNode node) {
        output.append(formatExpression(node));
    }

    @Override
    public void visit(NumberNode node) {
        output.append(node.value);
    }

    @Override
    public void visit(IdentifierNode node) {
        output.append(node.name);
    }

    @Override
    public void visit(StringNode node) {
        output.append("\"").append(node.value).append("\"");
    }

    private void formatBranchBody(Node body) {
        if (body instanceof BlockNode b) {
            for (Node s : b.statements) {
                s.accept(this);
            }
        } else if (body != null) {
            body.accept(this);
        }
    }
}
