package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa o resultado da execução do programa no back-end (Interpretador de TAC).
 *
 * Preserva o estado de memória de todas as variáveis, garantindo que variáveis
 * de escopos diferentes (com sombreamento/shadowing) mantenham sua identidade
 * distinta e valores íntegros.
 */
public class ExecutionResult {

    private final List<String> outputLines = new ArrayList<>();
    private final Map<String, Object> memory = new HashMap<>();
    private int instructionsExecuted = 0;
    private Integer returnValue = null;

    public ExecutionResult() {
    }

    public void addOutput(String line) {
        outputLines.add(line);
    }

    public List<String> getOutputLines() {
        return Collections.unmodifiableList(outputLines);
    }

    public String getOutput() {
        return String.join(System.lineSeparator(), outputLines);
    }

    public void setVariable(String name, Object value) {
        memory.put(name, value);
    }

    /**
     * Retorna o valor de uma variável. Se a variável exata não for encontrada,
     * verifica se existe uma versão qualificada com escopo (ex.: x_1).
     */
    public Object getVariable(String name) {
        if (memory.containsKey(name)) {
            return memory.get(name);
        }
        for (Map.Entry<String, Object> entry : memory.entrySet()) {
            if (entry.getKey().startsWith(name + "_")) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Retorna o valor de uma variável em um nível específico de escopo.
     * Escopo 0 corresponde à variável global/externa; escopos > 0 aos blocos aninhados.
     */
    public Object getVariable(String name, int scopeLevel) {
        if (scopeLevel == 0) {
            if (memory.containsKey(name)) return memory.get(name);
            if (memory.containsKey(name + "_0")) return memory.get(name + "_0");
        }
        String scopedName = name + "_" + scopeLevel;
        if (memory.containsKey(scopedName)) {
            return memory.get(scopedName);
        }
        return memory.get(name);
    }

    /**
     * Retorna todas as instâncias existentes de uma variável por identificador de escopo.
     */
    public Map<String, Object> getVariableInstances(String baseName) {
        Map<String, Object> instances = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : memory.entrySet()) {
            String key = entry.getKey();
            if (key.equals(baseName) || key.matches("^" + java.util.regex.Pattern.quote(baseName) + "_\\d+$")) {
                instances.put(key, entry.getValue());
            }
        }
        return instances;
    }

    public boolean hasVariable(String name) {
        if (memory.containsKey(name)) return true;
        for (String key : memory.keySet()) {
            if (key.startsWith(name + "_")) return true;
        }
        return false;
    }

    public Map<String, Object> getMemory() {
        return Collections.unmodifiableMap(memory);
    }

    public int getInstructionsExecuted() {
        return instructionsExecuted;
    }

    public void setInstructionsExecuted(int count) {
        this.instructionsExecuted = count;
    }

    public Integer getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Integer returnValue) {
        this.returnValue = returnValue;
    }

    public void printSummary() {
        System.out.println("================================================================================");
        System.out.println("                 7. BACK-END / EXECUÇÃO DO PROGRAMA MICROC                      ");
        System.out.println("================================================================================");

        System.out.println("\n--- SAÍDA PRODUZIDA PELO PROGRAMA (PRINT): ---");
        if (outputLines.isEmpty()) {
            System.out.println("   (Nenhuma saída impressa)");
        } else {
            for (String line : outputLines) {
                System.out.println("   >> " + line);
            }
        }

        if (returnValue != null) {
            System.out.println("\n--- VALOR DE RETORNO DO PROGRAMA: ---");
            System.out.println("   Código de retorno (exit code / return): " + returnValue);
        }

        System.out.println("\n--- ESTADO FINAL DA MEMÓRIA DE VARIÁVEIS: ---");
        List<String> sortedKeys = new ArrayList<>(memory.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            // Não polui com temporárias internas no resumo, foca nas variáveis do usuário
            if (!key.matches("^t\\d+$")) {
                String scopeDesc = "";
                int uscore = key.lastIndexOf('_');
                if (uscore != -1 && key.substring(uscore + 1).matches("\\d+")) {
                    scopeDesc = " (escopo " + key.substring(uscore + 1) + ")";
                }
                System.out.printf("   %-12s = %s%s%n", key, memory.get(key), scopeDesc);
            }
        }

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("Total de Instruções TAC Executadas: %d%n", instructionsExecuted);
        System.out.println("Status da Execução                : SUCESSO (Programa concluído)");
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
