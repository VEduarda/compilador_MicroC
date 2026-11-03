package optimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma instrução do Código Intermediário de Três Endereços (TAC).
 *
 * Formatos suportados:
 *   - Atribuição Binária:  dest = op1 operador op2  (ex.: t1 = 10 * 2)
 *   - Atribuição Unária:   dest = operador op1     (ex.: t1 = -a, t1 = !cond)
 *   - Atribuição de Cópia: dest = op1              (ex.: t3 = t2, x = 25)
 *   - Rótulo:              Label:                  (ex.: L1:, main:)
 *   - Salto Incondicional: goto Label              (ex.: goto L2)
 *   - Salto Condicional:   ifFalse cond goto Label (ex.: ifFalse t1 goto L1)
 *   - Salto Condicional:   ifTrue cond goto Label  (ex.: ifTrue t1 goto L1)
 *   - Saída:               print op1               (ex.: print x)
 *   - Retorno:             return [op1]            (ex.: return a, return)
 */
public class TACInstruction {

    public enum Kind {
        ASSIGN_BIN,    // dest = op1 op op2
        ASSIGN_UNARY,  // dest = op op1
        ASSIGN_COPY,   // dest = op1
        LABEL,         // label:
        GOTO,          // goto label
        IF_FALSE,      // ifFalse cond goto label
        IF_TRUE,       // ifTrue cond goto label
        PRINT,         // print op1
        RETURN,        // return [op1]
        COMMENT        // // comentario
    }

    public Kind kind;
    public String dest;
    public String op1;
    public String op;
    public String op2;
    public String label;
    public String comment;

    public TACInstruction(Kind kind) {
        this.kind = kind;
    }

    /**
     * Converte uma linha de texto TAC em uma TACInstruction estruturada.
     */
    public static TACInstruction parse(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return null;

        TACInstruction inst = new TACInstruction(Kind.COMMENT);

        // Comentários isolados
        if (trimmed.startsWith("//")) {
            inst.kind = Kind.COMMENT;
            inst.comment = trimmed;
            return inst;
        }

        // Separa comentário de final de linha, se houver
        String code = trimmed;
        int commentIdx = trimmed.indexOf("//");
        if (commentIdx != -1) {
            code = trimmed.substring(0, commentIdx).trim();
            inst.comment = trimmed.substring(commentIdx);
        }

        // Rótulo: "L1:" ou "main:"
        if (code.endsWith(":") && !code.contains(" ")) {
            inst.kind = Kind.LABEL;
            inst.label = code.substring(0, code.length() - 1);
            return inst;
        }

        // Salto incondicional: "goto L1"
        if (code.startsWith("goto ")) {
            inst.kind = Kind.GOTO;
            inst.label = code.substring(5).trim();
            return inst;
        }

        // Salto condicional: "ifFalse <cond> goto <label>"
        if (code.startsWith("ifFalse ")) {
            inst.kind = Kind.IF_FALSE;
            String rest = code.substring(8).trim();
            int gIdx = rest.indexOf("goto ");
            if (gIdx != -1) {
                inst.op1 = rest.substring(0, gIdx).trim();
                inst.label = rest.substring(gIdx + 5).trim();
            }
            return inst;
        }

        // Salto condicional: "ifTrue <cond> goto <label>" ou "if <cond> goto <label>"
        if (code.startsWith("ifTrue ")) {
            inst.kind = Kind.IF_TRUE;
            String rest = code.substring(7).trim();
            int gIdx = rest.indexOf("goto ");
            if (gIdx != -1) {
                inst.op1 = rest.substring(0, gIdx).trim();
                inst.label = rest.substring(gIdx + 5).trim();
            }
            return inst;
        }

        // Comando print: "print <expr>"
        if (code.startsWith("print ") || code.equals("print")) {
            inst.kind = Kind.PRINT;
            inst.op1 = code.length() > 5 ? code.substring(6).trim() : "";
            return inst;
        }

        // Comando return: "return <expr>" ou "return"
        if (code.startsWith("return ") || code.equals("return")) {
            inst.kind = Kind.RETURN;
            inst.op1 = code.length() > 6 ? code.substring(7).trim() : null;
            return inst;
        }

        // Atribuições: "dest = ..."
        int eqIdx = code.indexOf('=');
        if (eqIdx != -1) {
            inst.dest = code.substring(0, eqIdx).trim();
            String rhs = code.substring(eqIdx + 1).trim();

            // Verifica operadores binários conhecidos: +, -, *, /, ==, !=, <=, >=, <, >, &&, ||
            String[] binOps = {"==", "!=", "<=", ">=", "&&", "||", "+", "-", "*", "/", "<", ">"};
            boolean foundBin = false;

            for (String bOp : binOps) {
                // Procura o operador isolado por espaços para não confundir com prefixos unários
                int opIdx = rhs.indexOf(" " + bOp + " ");
                if (opIdx != -1) {
                    inst.kind = Kind.ASSIGN_BIN;
                    inst.op1 = rhs.substring(0, opIdx).trim();
                    inst.op = bOp;
                    inst.op2 = rhs.substring(opIdx + bOp.length() + 2).trim();
                    foundBin = true;
                    break;
                }
            }

            if (!foundBin) {
                // Operador unário: ex.: "- a", "! cond"
                if (rhs.startsWith("float ")) {
                    inst.kind = Kind.ASSIGN_UNARY;
                    inst.op = "float";
                    inst.op1 = rhs.substring(6).trim();
                } else if (rhs.startsWith("- ") || rhs.startsWith("! ")) {
                    inst.kind = Kind.ASSIGN_UNARY;
                    inst.op = rhs.substring(0, 1);
                    inst.op1 = rhs.substring(2).trim();
                } else if ((rhs.startsWith("-") || rhs.startsWith("!")) && !isInteger(rhs)) {
                    inst.kind = Kind.ASSIGN_UNARY;
                    inst.op = rhs.substring(0, 1);
                    inst.op1 = rhs.substring(1).trim();
                } else {
                    // Atribuição de cópia direta: dest = op1
                    inst.kind = Kind.ASSIGN_COPY;
                    inst.op1 = rhs;
                }
            }
            return inst;
        }

        inst.kind = Kind.COMMENT;
        inst.comment = "// " + code;
        return inst;
    }

    /**
     * Retorna a representação TAC canônica desta instrução.
     */
    public String toTAC() {
        return switch (kind) {
            case LABEL -> label + ":";
            case GOTO -> "goto " + label;
            case IF_FALSE -> "ifFalse " + op1 + " goto " + label;
            case IF_TRUE -> "ifTrue " + op1 + " goto " + label;
            case PRINT -> "print " + op1;
            case RETURN -> (op1 != null && !op1.isEmpty()) ? ("return " + op1) : "return";
            case ASSIGN_COPY -> dest + " = " + op1;
            case ASSIGN_UNARY -> dest + " = " + op + " " + op1;
            case ASSIGN_BIN -> dest + " = " + op1 + " " + op + " " + op2;
            case COMMENT -> comment != null ? comment : "";
        };
    }

    /**
     * Retorna o nome da variável ou temporária definida (escrita) por esta instrução, se houver.
     */
    public String getDef() {
        if (kind == Kind.ASSIGN_BIN || kind == Kind.ASSIGN_UNARY || kind == Kind.ASSIGN_COPY) {
            return dest;
        }
        return null;
    }

    /**
     * Retorna a lista de variáveis e temporárias lidas (usadas) por esta instrução.
     */
    public List<String> getUses() {
        List<String> uses = new ArrayList<>();
        switch (kind) {
            case ASSIGN_BIN -> {
                if (isVariable(op1)) uses.add(op1);
                if (isVariable(op2)) uses.add(op2);
            }
            case ASSIGN_UNARY, ASSIGN_COPY, PRINT, RETURN, IF_FALSE, IF_TRUE -> {
                if (op1 != null && isVariable(op1)) uses.add(op1);
            }
            default -> {}
        }
        return uses;
    }

    /**
     * Verifica se o identificador informado é uma variável ou temporária (não constante literal).
     */
    public static boolean isVariable(String name) {
        if (name == null || name.isEmpty()) return false;
        if (name.startsWith("\"") && name.endsWith("\"")) return false;
        return !isNumeric(name);
    }

    /** Reconhece literais inteiros e decimais para não tratá-los como variáveis TAC. */
    public static boolean isNumeric(String value) {
        if (isInteger(value)) return true;
        if (value == null || value.isBlank()) return false;
        try {
            Double.parseDouble(value.trim());
            return value.trim().contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verifica se o identificador é uma temporária gerada pelo compilador (t1, t2, ...).
     */
    public static boolean isTemporary(String name) {
        if (name == null) return false;
        return name.matches("^t\\d+$");
    }

    /**
     * Verifica se a string é um número inteiro.
     */
    public static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return toTAC();
    }
}
