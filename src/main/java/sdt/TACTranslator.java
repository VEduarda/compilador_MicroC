package sdt;

import ast.ASTVisitor;
import ast.Node;
import ast.Nodes.*;
import semantic.Symbol;
import semantic.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tradutor 2: TACTranslator (Gerador de Código Intermediário de Três Endereços - TAC / IR).
 *
 * Implementa o percurso da AST via padrão Visitor para sintetizar instruções
 * lineares no formato padrão de três endereços:
 *     destino = op1 operador op2
 *     destino = op1
 *     ifFalse cond goto Label
 *     goto Label
 *     Label:
 *     print x
 *
 * Variáveis de Estado Obrigatórias (Seção 3 e 4 do Enunciado):
 *   - tempCount   : contador incremental de temporárias.
 *   - labelCount  : contador incremental para geração de rótulos de desvio (L1, L2, L3...).
 *   - lastPlace   : sintetizadora de endereços entre nós filhos e nós pais.
 *   - instructions: fluxo linear de instruções intermediárias emitidas.
 *   - actionLog   : registro detalhado e didático de ações semânticas acionadas.
 *
 * Melhoria 2 Implementada:
 *   - Integração com TempPool: gerencia o ciclo de vida dos registradores temporários,
 *     devolvendo ao pool qualquer temporária lida por uma operação pai e permitindo
 *     seu reuso imediato pelas próximas instruções.
 *
 * Melhoria 3 Implementada:
 *   - Constant Folding (Dobramento de Constantes): no método visit(BinOpNode),
 *     avalia se ambos os filhos são NumberNode (ou constantes conhecidas na Tabela
 *     de Símbolos). Se forem, calcula o valor na hora (tempo de compilação)
 *     e guarda o resultado em lastPlace como string, sem alocar temporárias (newTemp)
 *     e sem emitir instruções intermediárias (emit).
 */
public class TACTranslator implements ASTVisitor {

    /** Contador incremental didático de variáveis temporárias (quando modo sem pool). */
    private int tempCount = 0;

    /** Contador incremental de rótulos de desvio (L1, L2, L3...). */
    private int labelCount = 0;

    /** Canal de comunicação sintetizador de endereços entre filho e pai. */
    private String lastPlace = "";

    /** Lista linear de instruções TAC geradas. */
    private final List<String> instructions = new ArrayList<>();

    /** Log de ações didáticas executadas durante a tradução dirigida por sintaxe. */
    private final List<String> actionLog = new ArrayList<>();

    /** Gerenciador de pool de registradores temporários (Melhoria 2). */
    private final TempPool tempPool;

    /** Flag indicando se a Melhoria 2 (reuso inteligente via pool) está ativada. */
    private boolean usePool = true;

    /** Flag indicando se a otimização de Constant Folding está ativada. */
    private boolean constantFolding = true;

    /** Tabela de símbolos opcional para resolução de constantes conhecidas. */
    private SymbolTable symbolTable;

    /** Mapa local de constantes conhecidas rastreadas durante a geração. */
    private final Map<String, Integer> knownConstants = new HashMap<>();

    /** Tipos declarados por escopo para emitir conversões int -> float no TAC. */
    private final Deque<Map<String, String>> typeScopes = new ArrayDeque<>();

    /**
     * Par de rótulos (L_start, L_end) do laço de repetição atual para desvios de break e continue (Melhoria 5).
     */
    public static class LoopLabels {
        public final String start;
        public final String end;

        public LoopLabels(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    /** Pilha de controle de laços aninhados para emissão de break e continue. */
    private final Deque<LoopLabels> loopStack = new ArrayDeque<>();

    /**
     * Construtor padrão com a Melhoria 2 (Pool de Temporárias) e Constant Folding ativados.
     */
    public TACTranslator() {
        this(new TempPool(), true, true, null);
    }

    /**
     * Construtor com Tabela de Símbolos para resolução semântica de constantes.
     */
    public TACTranslator(SymbolTable symbolTable) {
        this(new TempPool(), true, true, symbolTable);
    }

    /**
     * Construtor flexível que permite instanciar com ou sem pool para fins comparativos.
     */
    public TACTranslator(TempPool pool, boolean usePool) {
        this(pool, usePool, true, null);
    }

    /**
     * Construtor flexível que permite configurar pool e constant folding.
     */
    public TACTranslator(TempPool pool, boolean usePool, boolean constantFolding) {
        this(pool, usePool, constantFolding, null);
    }

    /**
     * Construtor completo com pool, constant folding e tabela de símbolos.
     */
    public TACTranslator(TempPool pool, boolean usePool, boolean constantFolding, SymbolTable symbolTable) {
        this.tempPool = (pool != null) ? pool : new TempPool();
        this.usePool = usePool;
        this.constantFolding = constantFolding;
        this.symbolTable = symbolTable;
    }

    // =========================================================================
    // Métodos Auxiliares Obrigatórios (Contrato Seção 4)
    // =========================================================================

    /**
     * Aloca uma nova variável temporária.
     * Quando usePool = true (Melhoria 2), consulta o pool para reaproveitar temporárias livres.
     */
    public String newTemp() {
        if (usePool) {
            String temp = tempPool.acquire();
            actionLog.add("[newTemp] " + temp + " obtido via TempPool (Melhoria 2 ativada)");
            return temp;
        } else {
            tempCount++;
            String temp = "t" + tempCount;
            actionLog.add("[newTemp] " + temp + " alocado sequencialmente (sem pool)");
            return temp;
        }
    }

    /**
     * Libera uma variável temporária de volta ao pool após ser lida por uma operação pai.
     */
    private void releaseTemp(String place) {
        if (usePool && tempPool.isTemp(place)) {
            tempPool.release(place);
            actionLog.add("[releaseTemp] " + place + " devolvido ao pool para reuso");
        }
    }

    /**
     * Gera um novo rótulo de desvio sequencial (L1, L2, L3...).
     */
    public String newLabel() {
        labelCount++;
        String label = "L" + labelCount;
        actionLog.add("[newLabel] Rótulo " + label + " gerado");
        return label;
    }

    /**
     * Ação semântica principal: emite uma instrução TAC no fluxo de texto e no actionLog.
     *
     * @param instruction código da instrução TAC
     * @param ruleNote    anotação didática da regra gramatical/semântica associada
     */
    public void emit(String instruction, String ruleNote) {
        instructions.add(instruction);
        String logEntry = "[EMIT] " + instruction;
        if (ruleNote != null && !ruleNote.isBlank()) {
            logEntry += "  \t// " + ruleNote;
        }
        actionLog.add(logEntry);
    }

    public void emit(String instruction) {
        emit(instruction, "");
    }

    // =========================================================================
    // Ponto de Entrada de Compilação / Geração
    // =========================================================================

    /**
     * Processa a árvore a partir do nó raiz informado e retorna a lista de instruções TAC.
     */
    public List<String> generate(Node node) {
        instructions.clear();
        actionLog.clear();
        loopStack.clear();
        tempCount = 0;
        labelCount = 0;
        lastPlace = "";
        knownConstants.clear();
        typeScopes.clear();
        typeScopes.push(new HashMap<>());

        if (usePool) {
            tempPool.reset();
        }

        if (node != null) {
            node.accept(this);
        }

        return new ArrayList<>(instructions);
    }

    /**
     * Imprime no console a listagem linear de instruções geradas.
     */
    public void printInstructions() {
        System.out.println("=== CÓDIGO INTERMEDIÁRIO DE TRÊS ENDEREÇOS (TAC / IR) ===");
        for (String inst : instructions) {
            System.out.println(inst);
        }
    }

    /**
     * Imprime no console o log didático das ações semânticas executadas.
     */
    public void printActionLog() {
        System.out.println("=== LOG DE AÇÕES SEMÂNTICAS DO TACTranslator ===");
        for (String log : actionLog) {
            System.out.println(log);
        }
    }

    // =========================================================================
    // Métodos Visitor Obrigatórios (Seção 4)
    // =========================================================================

    @Override
    public void visit(ProgramNode node) {
        actionLog.add("[visit ProgramNode] Início da tradução do programa com " 
                + node.statements.size() + " comandos");
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
        actionLog.add("[visit ProgramNode] Fim da tradução do programa");
    }

    @Override
    public void visit(DeclarationNode node) {
        // VarDeclNode
        actionLog.add("[visit DeclarationNode] Declaração de variável '" + node.name 
                + "' do tipo " + node.type);
        if (node.initializer != null) {
            node.initializer.accept(this);
            String val = lastPlace;
            if ("float".equals(node.type)) {
                val = convertToFloat(val);
            }
            emit(node.name + " = " + val, "VarDecl: inicialização de '" + node.name + "'");
            releaseTemp(val);
            recordKnownConstant(node.name, val);
        } else {
            actionLog.add("[DeclarationNode] Variável '" + node.name + "' sem valor inicial");
            knownConstants.remove(node.name);
        }
        typeScopes.peek().put(node.name, node.type);
    }

    @Override
    public void visit(AssignmentNode node) {
        // AssignNode
        actionLog.add("[visit AssignmentNode] Atribuição para variável '" + node.name + "'");
        node.value.accept(this);
        String val = lastPlace;
        if ("float".equals(lookupType(node.name))) {
            val = convertToFloat(val);
        }
        emit(node.name + " = " + val, "Assign: atribuição a '" + node.name + "'");
        releaseTemp(val);
        recordKnownConstant(node.name, val);
    }

    @Override
    public void visit(PrintNode node) {
        actionLog.add("[visit PrintNode] Comando print");
        node.expression.accept(this);
        String val = lastPlace;
        emit("print " + val, "Print");
        releaseTemp(val);
    }

    @Override
    public void visit(IfNode node) {
        actionLog.add("[visit IfNode] Estrutura condicional if" 
                + (node.elseBranch != null ? "/else" : ""));

        // 1. Avalia a expressão de condição -> resultado depositado em lastPlace
        node.condition.accept(this);
        String cond = lastPlace;

        if (node.elseBranch != null) {
            String lElse = newLabel();
            String lEnd = newLabel();

            emit("ifFalse " + cond + " goto " + lElse, "If-Else: desvio para L_else caso falso");
            // A condição foi avaliada pelo desvio; pode liberar registrador se for temporária
            releaseTemp(cond);

            // 2. Gera corpo then
            node.thenBranch.accept(this);
            emit("goto " + lEnd, "If-Else: salto incondicional para L_end");

            // 3. Rotula L_else:
            emit(lElse + ":", "If-Else: marcador de rótulo L_else");

            // 4. Gera corpo else
            node.elseBranch.accept(this);

            // 5. Rotula L_end:
            emit(lEnd + ":", "If-Else: marcador de rótulo L_end");

        } else {
            String lEnd = newLabel();

            emit("ifFalse " + cond + " goto " + lEnd, "If: desvio para L_end caso falso");
            releaseTemp(cond);

            node.thenBranch.accept(this);
            emit(lEnd + ":", "If: marcador de rótulo L_end");
        }
    }

    @Override
    public void visit(WhileNode node) {
        actionLog.add("[visit WhileNode] Estrutura de repetição while");

        String lStart = newLabel();
        String lEnd = newLabel();

        loopStack.push(new LoopLabels(lStart, lEnd));

        try {
            // 1. Rotula L_start:
            emit(lStart + ":", "While: marcador de rótulo L_start");

            // 2. Avalia a condição do laço
            node.condition.accept(this);
            String cond = lastPlace;

            // 3. Emite ifFalse cond goto L_end
            emit("ifFalse " + cond + " goto " + lEnd, "While: desvio de saída do laço");
            releaseTemp(cond);

            // 4. Gera corpo do laço
            node.body.accept(this);

            // 5. Emite goto L_start
            emit("goto " + lStart, "While: retorno incondicional para L_start");

            // 6. Rotula L_end:
            emit(lEnd + ":", "While: marcador de rótulo L_end");
        } finally {
            loopStack.pop();
        }
    }

    @Override
    public void visit(BreakNode node) {
        actionLog.add("[visit BreakNode] Comando break");
        if (loopStack.isEmpty()) {
            throw new RuntimeException("[ERRO TAC / SEMÂNTICO] Linha " + (node.line > 0 ? node.line : "?")
                    + ": break fora de laço");
        }
        emit("goto " + loopStack.peek().end, "Break: desvio para o fim do laço");
    }

    @Override
    public void visit(ContinueNode node) {
        actionLog.add("[visit ContinueNode] Comando continue");
        if (loopStack.isEmpty()) {
            throw new RuntimeException("[ERRO TAC / SEMÂNTICO] Linha " + (node.line > 0 ? node.line : "?")
                    + ": continue fora de laço");
        }
        emit("goto " + loopStack.peek().start, "Continue: desvio para o início do laço");
    }

    @Override
    public void visit(BinOpNode node) {
        // BinaryOpNode: avalia filhos e emite instrução de 3 endereços
        actionLog.add("[visit BinOpNode] Operação binária '" + node.op + "'");

        node.left.accept(this);
        String left = lastPlace;

        node.right.accept(this);
        String right = lastPlace;

        // Otimização: Constant Folding (Dobramento de Constantes)
        // Checa se os dois filhos são NumberNode (ou constantes conhecidas na tabela de símbolos) antes de emit()
        if (constantFolding) {
            Integer constLeft = resolveConstant(node.left, left);
            Integer constRight = resolveConstant(node.right, right);

            if (constLeft != null && constRight != null) {
                Integer result = computeConstant(node.op, constLeft, constRight);
                if (result != null) {
                    // Guarda o resultado em lastPlace como string - sem chamar newTemp() e sem emit()
                    lastPlace = String.valueOf(result);
                    actionLog.add("[visit BinOpNode] Constant Folding aplicado: " + left + " " + node.op + " " + right 
                            + " -> " + lastPlace + " (calculado na hora, sem newTemp)");
                    return;
                }
            }
        }

        String temp = newTemp();
        emit(temp + " = " + left + " " + node.op + " " + right, "BinOp: operador '" + node.op + "'");

        // Melhoria 2: Os registradores filhos acabaram de ser lidos pela operação pai.
        // São imediatamente devolvidos ao pool para que as próximas operações os reutilizem.
        releaseTemp(left);
        releaseTemp(right);

        lastPlace = temp;
    }

    @Override
    public void visit(NumberNode node) {
        // Constante numérica literal
        lastPlace = node.value;
        actionLog.add("[visit NumberNode] Constante literal " + node.value + " -> lastPlace");
    }

    @Override
    public void visit(IdentifierNode node) {
        // VariableNode
        lastPlace = node.name;
        actionLog.add("[visit IdentifierNode] Variável " + node.name + " -> lastPlace");
    }

    @Override
    public void visit(StringNode node) {
        lastPlace = "\"" + node.value + "\"";
        actionLog.add("[visit StringNode] String literal " + lastPlace + " -> lastPlace");
    }

    @Override
    public void visit(BlockNode node) {
        actionLog.add("[visit BlockNode] Início de bloco com " + node.statements.size() + " comandos");
        typeScopes.push(new HashMap<>());
        for (Node stmt : node.statements) {
            stmt.accept(this);
        }
        typeScopes.pop();
        actionLog.add("[visit BlockNode] Fim de bloco");
    }

    @Override
    public void visit(UnaryOpNode node) {
        actionLog.add("[visit UnaryOpNode] Operação unária '" + node.op + "'");
        node.expr.accept(this);
        String operand = lastPlace;

        if (constantFolding) {
            Integer c = resolveConstant(node.expr, operand);
            if (c != null) {
                int res = node.op.equals("!") ? (c == 0 ? 1 : 0) : -c;
                lastPlace = String.valueOf(res);
                actionLog.add("[visit UnaryOpNode] Constant Folding aplicado: " + node.op + operand + " -> " + lastPlace);
                return;
            }
        }

        String temp = newTemp();
        emit(temp + " = " + node.op + " " + operand, "UnaryOp: '" + node.op + "'");
        releaseTemp(operand);
        lastPlace = temp;
    }

    @Override
    public void visit(ReturnNode node) {
        actionLog.add("[visit ReturnNode] Comando return");
        if (node.value != null) {
            node.value.accept(this);
            String val = lastPlace;
            emit("return " + val, "Return com valor");
            releaseTemp(val);
        } else {
            emit("return", "Return sem valor");
        }
    }

    @Override
    public void visit(FunctionNode node) {
        actionLog.add("[visit FunctionNode] Início da função '" + node.name + "'");
        emit(node.name + ":", "Rótulo da função '" + node.name + "'");
        typeScopes.push(new HashMap<>());
        if (node.parameters != null) {
            for (DeclarationNode parameter : node.parameters) {
                typeScopes.peek().put(parameter.name, parameter.type);
            }
        }
        if (node.body != null) {
            node.body.accept(this);
        }
        typeScopes.pop();
        actionLog.add("[visit FunctionNode] Fim da função '" + node.name + "'");
    }

    private String convertToFloat(String value) {
        String temp = newTemp();
        emit(temp + " = float " + value, "Conversão implícita de int para float");
        releaseTemp(value);
        return temp;
    }

    private String lookupType(String name) {
        for (Map<String, String> scope : typeScopes) {
            String type = scope.get(name);
            if (type != null) return type;
        }
        return null;
    }

    // =========================================================================
    // Getters de Estado e Diagnóstico
    // =========================================================================

    public List<String> getInstructions() {
        return new ArrayList<>(instructions);
    }

    public List<String> getActionLog() {
        return new ArrayList<>(actionLog);
    }

    public String getLastPlace() {
        return lastPlace;
    }

    public TempPool getTempPool() {
        return tempPool;
    }

    public boolean isUsePool() {
        return usePool;
    }

    public void setUsePool(boolean usePool) {
        this.usePool = usePool;
    }

    public int getLabelCount() {
        return labelCount;
    }

    public int getTempCount() {
        return tempCount;
    }

    public boolean isConstantFolding() {
        return constantFolding;
    }

    public void setConstantFolding(boolean constantFolding) {
        this.constantFolding = constantFolding;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public Map<String, Integer> getKnownConstants() {
        return new HashMap<>(knownConstants);
    }

    // =========================================================================
    // Métodos Auxiliares de Constant Folding e Gerenciamento de Constantes
    // =========================================================================

    /**
     * Permite registrar explicitamente uma constante conhecida (ex.: const PI = 3).
     */
    public void registerConstant(String name, int value) {
        knownConstants.put(name, value);
        if (symbolTable != null) {
            Symbol sym = symbolTable.lookup(name);
            if (sym != null) {
                sym.constantValue = String.valueOf(value);
            }
        }
    }

    /**
     * Atualiza o valor constante na tabela de símbolos apenas se o símbolo for uma constante declarada.
     */
    private void recordKnownConstant(String name, String val) {
        if (symbolTable != null) {
            Symbol sym = symbolTable.lookup(name);
            if (sym != null && sym.isConstant()) {
                sym.constantValue = val;
                try {
                    knownConstants.put(name, Integer.parseInt(val));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Otimização Constant Folding:
     * Verifica se um operando é uma constante conhecida (NumberNode, constante na tabela de símbolos
     * ou valor numérico resultante de subexpressão já dobrada em tempo de compilação).
     *
     * @param child nó filho na AST
     * @param place string sintetizada em lastPlace para o filho
     * @return o valor numérico inteiro ou null se for dinâmico / variável mutável / temporária
     */
    private Integer resolveConstant(Node child, String place) {
        // 1. O filho é diretamente um NumberNode literal
        if (child instanceof NumberNode numberNode) {
            try {
                return Integer.parseInt(numberNode.value);
            } catch (NumberFormatException ignored) {}
        }

        // 2. O filho é um IdentifierNode correspondente a uma constante conhecida na tabela de símbolos
        if (child instanceof IdentifierNode idNode) {
            Integer val = lookupConstant(idNode.name);
            if (val != null) {
                return val;
            }
        }

        // 3. Subexpressão já dobrada cujo nó filho gerador foi uma operação entre constantes (BinOpNode)
        // e cujo lastPlace sintetizado contém um literal numérico
        if (child instanceof BinOpNode && place != null) {
            try {
                return Integer.parseInt(place);
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    /**
     * Consulta se um nome de identificador corresponde a uma constante conhecida na tabela de símbolos
     * ou no mapa local de constantes registradas.
     */
    private Integer lookupConstant(String name) {
        if (name == null || name.isBlank()) return null;

        // Consulta SymbolTable se fornecida: somente considera constante se o símbolo
        // for da categoria CONSTANT ou possuir isConstant() verdadeiro
        if (symbolTable != null) {
            Symbol sym = symbolTable.lookup(name);
            if (sym != null && sym.isConstant() && sym.constantValue != null) {
                try {
                    return Integer.parseInt(sym.constantValue);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Consulta mapa de constantes explicitamente registradas
        if (knownConstants.containsKey(name)) {
            return knownConstants.get(name);
        }

        return null;
    }

    /**
     * Calcula o valor da operação binária em tempo de compilação (Constant Folding).
     *
     * @param op    operador aritmético ou relacional
     * @param left  valor do operando esquerdo (stoi / Integer.parseInt)
     * @param right valor do operando direito (stoi / Integer.parseInt)
     * @return resultado inteiro da operação ou null se divisão por zero / operador desconhecido
     */
    private Integer computeConstant(String op, int left, int right) {
        return switch (op) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> (right != 0) ? (left / right) : null;
            case "<" -> (left < right) ? 1 : 0;
            case ">" -> (left > right) ? 1 : 0;
            case "<=" -> (left <= right) ? 1 : 0;
            case ">=" -> (left >= right) ? 1 : 0;
            case "==" -> (left == right) ? 1 : 0;
            case "!=" -> (left != right) ? 1 : 0;
            case "&&" -> ((left != 0) && (right != 0)) ? 1 : 0;
            case "||" -> ((left != 0) || (right != 0)) ? 1 : 0;
            default -> null;
        };
    }
}
