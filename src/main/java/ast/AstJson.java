package ast;

import ast.Nodes.*;

/**
 * Responsável por converter a AST (Abstract Syntax Tree)
 * para uma representação textual no formato JSON.
 *
 * Esta funcionalidade foi implementada como melhoria
 * adicional do projeto.
 *
 * O JSON gerado permite:
 *
 *   - Visualizar a estrutura da árvore sintática.
 *   - Facilitar depuração do compilador.
 *   - Integrar futuramente com interpretadores.
 *   - Exportar resultados para outras ferramentas.
 */
public class AstJson {

    /**
     * Converte recursivamente um nó da AST para JSON.
     */
    public static String toJson(Node node) {

        // =====================================================
        // PROGRAM
        // =====================================================

        if (node instanceof ProgramNode p) {

            StringBuilder json = new StringBuilder();

            json.append("{\n");
            json.append("  \"Program\": [\n");

            for (int i = 0; i < p.statements.size(); i++) {

                json.append(indent(
                        toJson(p.statements.get(i)),
                        4));

                if (i < p.statements.size() - 1) {
                    json.append(",");
                }

                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();
        }

        // =====================================================
        // DECLARATION
        // =====================================================

        if (node instanceof DeclarationNode d) {

            String initializer =
                    d.initializer == null
                            ? "null"
                            : toJson(d.initializer);

            return """
                    {
                      "Declaration": {
                        "type": "%s",
                        "name": "%s",
                        "initializer": %s
                      }
                    }
                    """.formatted(
                    d.type,
                    d.name,
                    initializer);
        }

        // =====================================================
        // ASSIGNMENT
        // =====================================================

        if (node instanceof AssignmentNode a) {

            return """
                    {
                      "Assignment": {
                        "variable": "%s",
                        "value": %s
                      }
                    }
                    """.formatted(
                    a.name,
                    toJson(a.value));
        }

        // =====================================================
        // PRINT
        // =====================================================

        if (node instanceof PrintNode p) {

            return """
                    {
                      "Print": %s
                    }
                    """.formatted(
                    toJson(p.expression));
        }

        // =====================================================
        // BLOCK
        // =====================================================

        if (node instanceof BlockNode b) {

            StringBuilder json =
                    new StringBuilder();

            json.append("{\n");
            json.append("  \"Block\": [\n");

            for (int i = 0; i < b.statements.size(); i++) {

                json.append(indent(
                        toJson(
                                b.statements.get(i)),
                        4));

                if (i < b.statements.size() - 1) {
                    json.append(",");
                }

                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();
        }

        // =====================================================
        // IF
        // =====================================================

        if (node instanceof IfNode i) {

            String elseBranch =
                    i.elseBranch == null
                            ? "null"
                            : toJson(i.elseBranch);

            return """
                    {
                      "If": {
                        "condition": %s,
                        "then": %s,
                        "else": %s
                      }
                    }
                    """.formatted(
                    toJson(i.condition),
                    toJson(i.thenBranch),
                    elseBranch);
        }

        // =====================================================
        // WHILE
        // =====================================================

        if (node instanceof WhileNode w) {

            return """
                    {
                      "While": {
                        "condition": %s,
                        "body": %s
                      }
                    }
                    """.formatted(
                    toJson(w.condition),
                    toJson(w.body));
        }

        // =====================================================
        // BREAK
        // =====================================================

        if (node instanceof BreakNode) {
            return """
                    {
                      "Break": true
                    }
                    """;
        }

        // =====================================================
        // CONTINUE
        // =====================================================

        if (node instanceof ContinueNode) {
            return """
                    {
                      "Continue": true
                    }
                    """;
        }

        // =====================================================
        // BIN OP
        // =====================================================

        if (node instanceof BinOpNode b) {

            return """
                    {
                      "BinOp": {
                        "operator": "%s",
                        "left": %s,
                        "right": %s
                      }
                    }
                    """.formatted(
                    b.op,
                    toJson(b.left),
                    toJson(b.right));
        }

        // =====================================================
        // NUMBER
        // =====================================================

        if (node instanceof NumberNode n) {

            return """
                    {
                      "Number": {
                        "value": "%s",
                        "type": "%s"
                      }
                    }
                    """.formatted(
                    n.value,
                    n.type);
        }

        // =====================================================
        // STRING
        // =====================================================

        if (node instanceof StringNode s) {

            return """
                    {
                      "String": "%s"
                    }
                    """.formatted(
                    s.value);
        }

        // =====================================================
        // IDENTIFIER
        // =====================================================

        if (node instanceof IdentifierNode i) {

            return """
                    {
                      "Identifier": "%s"
                    }
                    """.formatted(
                    i.name);
        }

        if (node instanceof ReturnNode r) {
            String val = r.value == null ? "null" : toJson(r.value);
            return """
                    {
                      "Return": {
                        "value": %s
                      }
                    }
                    """.formatted(val);
        }

        if (node instanceof FunctionNode f) {
            return """
                    {
                      "Function": {
                        "returnType": "%s",
                        "name": "%s",
                        "body": %s
                      }
                    }
                    """.formatted(f.returnType, f.name, toJson(f.body));
        }

        if (node instanceof UnaryOpNode u) {
            return """
                    {
                      "UnaryOp": {
                        "op": "%s",
                        "expr": %s
                      }
                    }
                    """.formatted(u.op, toJson(u.expr));
        }

        return "{}";
    }

    /**
     * Adiciona espaços à esquerda para melhorar
     * a visualização do JSON gerado.
     */
    private static String indent(
            String text,
            int spaces) {

        String prefix =
                " ".repeat(spaces);

        String[] lines =
                text.split("\n");

        StringBuilder result =
                new StringBuilder();

        for (int i = 0; i < lines.length; i++) {

            result.append(prefix)
                  .append(lines[i]);

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}