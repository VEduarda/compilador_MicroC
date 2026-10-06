package sdt;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * Melhoria 2: Reuso Inteligente de Variáveis Temporárias (Temporary Register Pool).
 *
 * Em vez de gerar registradores temporários infinitos (t1, t2, t3, ..., t50),
 * gerencia um conjunto de variáveis temporárias reutilizáveis.
 *
 * Funcionamento:
 * 1. Ao solicitar um registrador temporário (acquire()), verifica se há algum
 *    previamente liberado no pool (fila de prioridade pelo menor índice).
 * 2. Se houver, reutiliza-o imediatamente (ex.: t1).
 * 3. Caso contrário, instancia um novo registrador incremental (t1, t2, ...).
 * 4. Assim que um registrador temporário é lido/consumido por uma operação pai
 *    (seja uma operação binária, atribuição, condicional ou impressão), ele é
 *    devolvido ao pool via release(nome), ficando pronto para a próxima expressão.
 *
 * Registra todas as ações e fornece métricas didáticas de economia de registradores.
 */
public class TempPool {

    /** Fila de prioridade contendo os IDs de temporárias livres para reuso (sempre menor primeiro). */
    private final PriorityQueue<Integer> freeTemps = new PriorityQueue<>();

    /** Conjunto de temporárias atualmente ativas (em uso na árvore de expressão). */
    private final TreeSet<Integer> activeTemps = new TreeSet<>();

    /** Maior índice de temporária criado até o momento (pico de registradores). */
    private int maxCreated = 0;

    /** Contagem total de solicitações de temporárias efetuadas pelo compilador. */
    private int totalAllocations = 0;

    /** Contagem de solicitações atendidas por reuso de temporárias livres. */
    private int totalReuses = 0;

    /** Log didático das operações do pool de temporárias. */
    private final List<String> poolLog = new ArrayList<>();

    public TempPool() {
        reset();
    }

    /**
     * Adquire um registrador temporário para armazenar o resultado de uma operação.
     * Reutiliza o menor registrador livre disponível ou cria um novo se todos estiverem ocupados.
     *
     * @return nome da variável temporária (ex.: "t1", "t2")
     */
    public String acquire() {
        totalAllocations++;
        int id;

        if (!freeTemps.isEmpty()) {
            id = freeTemps.poll();
            activeTemps.add(id);
            totalReuses++;
            String name = "t" + id;
            poolLog.add("[Pool] REUSO: registrador " + name + " reutilizado do pool (Alocação #" 
                    + totalAllocations + ", Reusos acumulados: " + totalReuses + ")");
            return name;
        } else {
            maxCreated++;
            id = maxCreated;
            activeTemps.add(id);
            String name = "t" + id;
            poolLog.add("[Pool] NOVO: registrador " + name + " criado (Novo pico de temporárias ativas: " 
                    + maxCreated + ")");
            return name;
        }
    }

    /**
     * Devolve um registrador temporário ao pool após ser lido por um nó pai.
     *
     * @param tempName nome da variável temporária (ex.: "t1")
     */
    public void release(String tempName) {
        if (tempName == null || !isTemp(tempName)) {
            return;
        }

        try {
            int id = Integer.parseInt(tempName.substring(1));
            if (activeTemps.remove(id)) {
                if (!freeTemps.contains(id)) {
                    freeTemps.add(id);
                }
                poolLog.add("[Pool] LIBERAÇÃO: registrador " + tempName 
                        + " devolvido ao pool (Disponível para a próxima expressão)");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * Verifica se o identificador informado é uma variável temporária gerada pelo compilador.
     */
    public boolean isTemp(String name) {
        return name != null && name.matches("^t\\d+$");
    }

    /**
     * Verifica se uma temporária específica está atualmente ativa em memória.
     */
    public boolean isActive(String name) {
        if (!isTemp(name)) return false;
        try {
            int id = Integer.parseInt(name.substring(1));
            return activeTemps.contains(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Reinicializa o estado do pool para uma nova compilação.
     */
    public void reset() {
        freeTemps.clear();
        activeTemps.clear();
        maxCreated = 0;
        totalAllocations = 0;
        totalReuses = 0;
        poolLog.clear();
    }

    public int getMaxCreated() {
        return maxCreated;
    }

    public int getTotalAllocations() {
        return totalAllocations;
    }

    public int getTotalReuses() {
        return totalReuses;
    }

    public int getActiveCount() {
        return activeTemps.size();
    }

    public int getFreeCount() {
        return freeTemps.size();
    }

    public List<String> getPoolLog() {
        return new ArrayList<>(poolLog);
    }

    /**
     * Gera relatório comparativo de eficiência demonstrando a economia do pool de temporárias.
     */
    public String getSummaryReport() {
        int naiveTotal = totalAllocations;
        int saved = naiveTotal - maxCreated;
        double economy = naiveTotal > 0 ? ((double) saved / naiveTotal) * 100.0 : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTATÍSTICAS DO POOL DE TEMPORÁRIAS (MELHORIA 2) ===\n");
        sb.append(String.format("  - Total de solicitações de temporárias : %d%n", totalAllocations));
        sb.append(String.format("  - Total de reusos aproveitados do pool : %d%n", totalReuses));
        sb.append(String.format("  - Pico máximo de temporárias em uso    : %d (ex.: t1..t%d)%n", maxCreated, maxCreated));
        sb.append(String.format("  - Sem o pool seriam geradas            : %d temporárias (t1..t%d)%n", naiveTotal, naiveTotal));
        sb.append(String.format("  - Registradores economizados           : %d (%.1f%% de economia)%n", saved, economy));
        return sb.toString();
    }
}
