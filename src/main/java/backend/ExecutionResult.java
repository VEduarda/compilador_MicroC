package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa o resultado da execução do programa no back-end (Interpretador de TAC).
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

    public Object getVariable(String name) {
        return memory.get(name);
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
                System.out.printf("   %-12s = %s%n", key, memory.get(key));
            }
        }

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("Total de Instruções TAC Executadas: %d%n", instructionsExecuted);
        System.out.println("Status da Execução                : SUCESSO (Programa concluído)");
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
