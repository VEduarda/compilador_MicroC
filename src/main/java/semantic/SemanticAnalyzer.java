package semantic;

import ast.Node;
import ast.Nodes.*;
import semantic.Symbol.Category;

import java.util.ArrayList;
import java.util.List;

/*
 * Analisador Semântico do MicroC.
 *
 * Percorre a AST produzida pelo Parser (percurso em profundidade,
 * o mesmo padrão usado em print()/AstJson) e, para cada nó,
 * aplica as regras semânticas da linguagem:
 *
 *   R1 - Toda variável deve ser declarada antes de usada.
 *   R2 - Não pode haver duas declarações do mesmo nome no
 *        mesmo escopo (redeclaração).
 *   R3 - O tipo do valor atribuído (na declaração ou em uma
 *        atribuição) deve ser compatível com o tipo da variável.
 *   R4 - Os operandos de uma operação binária devem ter tipos
 *        compatíveis entre si (ex.: não se pode somar int + string).
 *   R5 - Um identificador com o mesmo nome pode ser redeclarado
 *        em um escopo mais interno (sombreamento / shadowing);
 *        isso NÃO é erro, apenas cria uma nova entrada que
 *        "esconde" a variável externa dentro daquele bloco.
 *   R6 - A condição de um if/while deve ser uma expressão válida
 *        (não pode ser diretamente uma string).
 *
 * A cada bloco { } (if, while ou o programa) corresponde um novo
 * escopo na Tabela de Símbolos (ver semantic.SymbolTable).
 *
 * Uso típico:
 *
 *     SemanticAnalyzer analyzer = new SemanticAnalyzer();
 *     List<SemanticError> erros = analyzer.analyze(programNode);
 *     SymbolTable ts = analyzer.getSymbolTable();
 */
public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();
    private final List<SemanticError> errors = new ArrayList<>();

    /* Tipos válidos reconhecidos por este subconjunto do MicroC. */
    private static final String TYPE_INT = "int";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_ERROR = "erro";

    /*
     * Ponto de entrada da análise semântica.
     * Retorna a lista de erros encontrados (vazia se o programa
     * é semanticamente válido).
     */
    public List<SemanticError> analyze(ProgramNode program) {

        for (Node stmt : program.statements) {
            visitStatement(stmt);
        }

        return errors;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<SemanticError> getErrors() {
        return errors;
    }

    // ------------------------------------------------------------------
    // Despacho de comandos (statements)
    // ------------------------------------------------------------------

    private void visitStatement(Node node) {

        if (node instanceof DeclarationNode d) {
            visitDeclaration(d);

        } else if (node instanceof AssignmentNode a) {
            visitAssignment(a);

        } else if (node instanceof PrintNode p) {
            visitPrint(p);

        } else if (node instanceof BlockNode b) {
            visitBlock(b);

        } else if (node instanceof IfNode i) {
            visitIf(i);

        } else if (node instanceof WhileNode w) {
            visitWhile(w);

        } else {
            error(node.line, "R0-ComandoDesconhecido",
                    "Comando não reconhecido pelo analisador semântico: "
                            + node.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // Declarações  ->  int x;  ou  int x = <expr>;
    // ------------------------------------------------------------------

    private void visitDeclaration(DeclarationNode node) {

        // R2 - redeclaração no mesmo escopo
        if (symbolTable.declaredInCurrentScope(node.name)) {

            error(node.line, "R2-Redeclaracao",
                    "variável '" + node.name
                            + "' já declarada anteriormente neste mesmo escopo");

            // Mesmo com erro, seguimos analisando o inicializador
            // para não mascarar outros problemas do programa.
            if (node.initializer != null) {
                inferType(node.initializer);
            }
            return;
        }

        boolean initialized = node.initializer != null;

        if (initialized) {

            String initType = inferType(node.initializer);

            // R3 - compatibilidade de tipos na inicialização
            if (!initType.equals(TYPE_ERROR)
                    && !initType.equals(node.type)) {

                error(node.line, "R3-TipoIncompativel",
                        "não é possível inicializar variável '" + node.name
                                + "' do tipo '" + node.type
                                + "' com um valor do tipo '" + initType + "'");
            }
        }

        Symbol symbol = new Symbol(
                node.name,
                node.type,
                Category.VARIABLE,
                symbolTable.currentScopeLevel(),
                node.line,
                initialized);

        symbolTable.declare(symbol);
    }

    // ------------------------------------------------------------------
    // Atribuições  ->  x = <expr>;
    // ------------------------------------------------------------------

    private void visitAssignment(AssignmentNode node) {

        Symbol symbol = symbolTable.lookup(node.name);

        // R1 - uso (aqui, atribuição) de variável não declarada
        if (symbol == null) {

            error(node.line, "R1-NaoDeclarada",
                    "variável '" + node.name
                            + "' não foi declarada antes de ser usada");

            // Ainda assim analisa o lado direito, para reportar
            // outros erros que possam existir na expressão.
            inferType(node.value);
            return;
        }

        String valueType = inferType(node.value);

        // R3 - compatibilidade de tipos na atribuição
        if (!valueType.equals(TYPE_ERROR)
                && !valueType.equals(symbol.type)) {

            error(node.line, "R3-TipoIncompativel",
                    "não é possível atribuir um valor do tipo '" + valueType
                            + "' à variável '" + node.name
                            + "', que é do tipo '" + symbol.type + "'");
        }

        symbol.initialized = true;
    }

    // ------------------------------------------------------------------
    // print(<expr>);
    // ------------------------------------------------------------------

    private void visitPrint(PrintNode node) {
        // print aceita qualquer tipo válido; só precisamos
        // validar a expressão (identificadores declarados, etc.).
        inferType(node.expression);
    }

    // ------------------------------------------------------------------
    // Blocos { ... }  ->  novo escopo
    // ------------------------------------------------------------------

    private void visitBlock(BlockNode node) {

        symbolTable.enterScope();

        for (Node stmt : node.statements) {
            visitStatement(stmt);
        }

        symbolTable.exitScope();
    }

    // ------------------------------------------------------------------
    // if (<cond>) { ... } else { ... }
    // ------------------------------------------------------------------

    private void visitIf(IfNode node) {

        checkCondition(node.condition);

        visitStatement(node.thenBranch);

        if (node.elseBranch != null) {
            visitStatement(node.elseBranch);
        }
    }

    // ------------------------------------------------------------------
    // while (<cond>) { ... }
    // ------------------------------------------------------------------

    private void visitWhile(WhileNode node) {

        checkCondition(node.condition);

        visitStatement(node.body);
    }

    /*
     * R6 - a condição de um if/while não pode ser uma string
     * (no MicroC não existe tipo booleano; usamos 'int' também
     * para o resultado de comparações, como em C).
     */
    private void checkCondition(Node condition) {

        String type = inferType(condition);

        if (type.equals(TYPE_STRING)) {

            error(condition.line, "R6-CondicaoInvalida",
                    "a condição de if/while não pode ser do tipo 'string'");
        }
    }

    // ------------------------------------------------------------------
    // Inferência e checagem de tipos em expressões
    // ------------------------------------------------------------------

    /*
     * Determina o tipo de uma expressão, ao mesmo tempo em que
     * valida as regras semânticas aplicáveis a ela (variável
     * declarada, operandos compatíveis, etc.).
     *
     * Retorna TYPE_ERROR quando a expressão já contém um erro
     * semântico, para evitar reportar o mesmo problema em
     * cascata em cada nível da árvore.
     */
    private String inferType(Node node) {

        if (node instanceof NumberNode) {
            return TYPE_INT;
        }

        if (node instanceof StringNode) {
            return TYPE_STRING;
        }

        if (node instanceof IdentifierNode id) {
            return inferIdentifierType(id);
        }

        if (node instanceof BinOpNode bin) {
            return inferBinOpType(bin);
        }

        error(node.line, "R0-ExpressaoDesconhecida",
                "expressão não reconhecida pelo analisador semântico: "
                        + node.getClass().getSimpleName());

        return TYPE_ERROR;
    }

    private String inferIdentifierType(IdentifierNode id) {

        Symbol symbol = symbolTable.lookup(id.name);

        // R1 - uso de variável não declarada
        if (symbol == null) {

            error(id.line, "R1-NaoDeclarada",
                    "variável '" + id.name + "' usada sem ter sido declarada");

            return TYPE_ERROR;
        }

        // Checagem extra (não pedida explicitamente no PDF, mas é
        // uma boa prática): usar uma variável antes de inicializar.
        if (!symbol.initialized) {

            error(id.line, "R7-UsoAntesInicializacao",
                    "variável '" + id.name
                            + "' está sendo usada antes de receber um valor");
        }

        return symbol.type;
    }

    private String inferBinOpType(BinOpNode bin) {

        String leftType = inferType(bin.left);
        String rightType = inferType(bin.right);

        // Se um dos lados já tem erro, não reportamos erro duplicado.
        if (leftType.equals(TYPE_ERROR) || rightType.equals(TYPE_ERROR)) {
            return TYPE_ERROR;
        }

        // R4 - operandos devem ter o mesmo tipo
        if (!leftType.equals(rightType)) {

            error(bin.line, "R4-OperandosIncompativeis",
                    "operador '" + bin.op + "' não pode ser aplicado entre '"
                            + leftType + "' e '" + rightType + "'");

            return TYPE_ERROR;
        }

        // Operações aritméticas/relacionais não fazem sentido entre
        // strings neste subconjunto da linguagem (não há concatenação).
        if (leftType.equals(TYPE_STRING)) {

            error(bin.line, "R4-OperadorInvalidoParaTipo",
                    "operador '" + bin.op + "' não é suportado para o tipo 'string'");

            return TYPE_ERROR;
        }

        // int op int -> int (relacionais também retornam int, como em C)
        return TYPE_INT;
    }

    // ------------------------------------------------------------------
    // Utilitário
    // ------------------------------------------------------------------

    private void error(int line, String rule, String message) {
        errors.add(new SemanticError(line, rule, message));
    }
}
