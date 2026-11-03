package sdt;

import ast.ASTVisitor;
import ast.Node;
import ast.Nodes.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tradutor 1: PostfixTranslator (Notação Polonesa Reversa / RPN).
 *
 * Percorre a AST aplicando um percurso estritamente pós-ordem nas subárvores de expressões.
 * Em cada operação binária:
 *   1. Visita o operando da esquerda;
 *   2. Visita o operando da direita;
 *   3. Emite o operador via ação semântica emit(token).
 *
 * A RPN elimina a necessidade de parênteses, pois a ordem de avaliação é intrínseca
 * à disposição linear dos operandos e operadores.
 *
 * Exemplo:
 *   Expressão: 3 + 4 * 5
 *   Saída RPN: 3 4 5 * +
 */
public class PostfixTranslator implements ASTVisitor {

    /** Lista linear de tokens RPN emitidos no fluxo atual. */
    private final List<String> tokens = new ArrayList<>();

    /** Log de ações didáticas registrando o disparo de cada ação semântica emit(). */
    private final List<String> actionLog = new ArrayList<>();

    /** Lista de expressões traduzidas individualmente (uma por comando/expressão). */
    private final List<String> expressionRPNs = new ArrayList<>();

    /**
     * Ação Semântica Principal (Seção 3): insere o token no fluxo e registra a ocorrência no actionLog didático.
     */
    public void emit(String token) {
        tokens.add(token);
        actionLog.add("[RPN emit] Token emitido: '" + token + "' (Posição no fluxo: " + tokens.size() + ")");
    }

    /**
     * Traduz um nó da AST (programa completo, comando ou expressão) gerando o fluxo RPN.
     */
    public String translate(Node node) {
        tokens.clear();
        actionLog.clear();
        expressionRPNs.clear();

        if (node != null) {
            node.accept(this);
        }

        return String.join(" ", tokens);
    }

    /**
     * Traduz uma expressão isolada e retorna a string em Notação Pós-fixa.
     * Exemplo: (2 + 3) * (10 - 4) -> "2 3 + 10 4 - *"
     */
    public String translateExpression(Node expr) {
        int beforeSize = tokens.size();
        if (expr != null) {
            expr.accept(this);
        }
        List<String> sub = new ArrayList<>();
        for (int i = beforeSize; i < tokens.size(); i++) {
            sub.add(tokens.get(i));
        }
        String result = String.join(" ", sub);
        expressionRPNs.add(result);
        return result;
    }

    public void printRPN() {
        System.out.println("=== NOTAÇÃO POLONESA REVERSA (RPN / PÓS-FIXA) ===");
        System.out.println(String.join(" ", tokens));
    }

    public void printActionLog() {
        System.out.println("=== LOG DE AÇÕES SEMÂNTICAS DO PostfixTranslator ===");
        for (String log : actionLog) {
            System.out.println(log);
        }
    }

    // =========================================================================
    // Métodos Visitor - Percurso Pós-Ordem em Expressões
    // =========================================================================

    @Override
    public void visit(ProgramNode node) {
        actionLog.add("[PostfixTranslator] Processando programa com " + node.statements.size() + " comandos");
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(DeclarationNode node) {
        actionLog.add("[PostfixTranslator] Declaração de " + node.name);
        if (node.initializer != null) {
            int startIdx = tokens.size();
            node.initializer.accept(this);
            captureExpression(startIdx, "Declaração de " + node.name);
        }
    }

    @Override
    public void visit(AssignmentNode node) {
        actionLog.add("[PostfixTranslator] Atribuição para " + node.name);
        int startIdx = tokens.size();
        node.value.accept(this);
        captureExpression(startIdx, "Atribuição a " + node.name);
    }

    @Override
    public void visit(PrintNode node) {
        actionLog.add("[PostfixTranslator] Comando print");
        int startIdx = tokens.size();
        node.expression.accept(this);
        captureExpression(startIdx, "Expressão do print");
    }

    @Override
    public void visit(IfNode node) {
        actionLog.add("[PostfixTranslator] Condicional if");
        int startIdx = tokens.size();
        node.condition.accept(this);
        captureExpression(startIdx, "Condição do if");

        node.thenBranch.accept(this);
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
        }
    }

    @Override
    public void visit(WhileNode node) {
        actionLog.add("[PostfixTranslator] Laço while");
        int startIdx = tokens.size();
        node.condition.accept(this);
        captureExpression(startIdx, "Condição do while");

        node.body.accept(this);
    }

    @Override
    public void visit(BreakNode node) {
        actionLog.add("[PostfixTranslator] Comando break");
    }

    @Override
    public void visit(ContinueNode node) {
        actionLog.add("[PostfixTranslator] Comando continue");
    }

    @Override
    public void visit(BlockNode node) {
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(BinOpNode node) {
        // 1. Visita operando esquerdo
        node.left.accept(this);
        // 2. Visita operando direito
        node.right.accept(this);
        // 3. Emite o operador após ambos os operandos (pós-ordem estrita)
        emit(node.op);
    }

    @Override
    public void visit(UnaryOpNode node) {
        if (node.expr != null) {
            node.expr.accept(this);
        }
        emit(node.op);
    }

    @Override
    public void visit(NumberNode node) {
        emit(node.value);
    }

    @Override
    public void visit(IdentifierNode node) {
        emit(node.name);
    }

    @Override
    public void visit(StringNode node) {
        emit("\"" + node.value + "\"");
    }

    @Override
    public void visit(ReturnNode node) {
        if (node.value != null) {
            int startIdx = tokens.size();
            node.value.accept(this);
            captureExpression(startIdx, "Expressão do return");
        }
    }

    @Override
    public void visit(FunctionNode node) {
        if (node.body != null) {
            node.body.accept(this);
        }
    }

    private void captureExpression(int startIdx, String label) {
        List<String> sub = new ArrayList<>();
        for (int i = startIdx; i < tokens.size(); i++) {
            sub.add(tokens.get(i));
        }
        if (!sub.isEmpty()) {
            String exprStr = String.join(" ", sub);
            expressionRPNs.add(exprStr);
            actionLog.add("[RPN Expressão] " + label + " -> " + exprStr);
        }
    }

    public List<String> getTokens() {
        return new ArrayList<>(tokens);
    }

    public List<String> getActionLog() {
        return new ArrayList<>(actionLog);
    }

    public List<String> getExpressionRPNs() {
        return new ArrayList<>(expressionRPNs);
    }

    public String getOutput() {
        return String.join(" ", tokens);
    }
}
