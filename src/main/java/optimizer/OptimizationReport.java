package optimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Registra o relatório completo da etapa de Otimização de Código (Item 5 do Enunciado).
 *
 * Apresenta:
 *   - Código Intermediário Original (Antes da Otimização)
 *   - Lista detalhada de transformações realizadas com justificativas
 *   - Código Intermediário Otimizado (Após a Otimização)
 *   - Estatísticas de redução de instruções e eficiência
 */
public class OptimizationReport {

    public static class Transformation {
        public final String technique;
        public final String before;
        public final String after;
        public final String explanation;

        public Transformation(String technique, String before, String after, String explanation) {
            this.technique = technique;
            this.before = before;
            this.after = after;
            this.explanation = explanation;
        }

        @Override
        public String toString() {
            if (after == null || after.isEmpty()) {
                return String.format("  [%s] '%s' -> REMOVIDO (%s)", technique, before, explanation);
            }
            return String.format("  [%s] '%s' -> '%s' (%s)", technique, before, after, explanation);
        }
    }

    private final List<String> originalInstructions = new ArrayList<>();
    private final List<String> optimizedInstructions = new ArrayList<>();
    private final List<Transformation> transformations = new ArrayList<>();

    public void setOriginalInstructions(List<String> instructions) {
        originalInstructions.clear();
        if (instructions != null) originalInstructions.addAll(instructions);
    }

    public void setOptimizedInstructions(List<String> instructions) {
        optimizedInstructions.clear();
        if (instructions != null) optimizedInstructions.addAll(instructions);
    }

    public void addTransformation(String technique, String before, String after, String explanation) {
        transformations.add(new Transformation(technique, before, after, explanation));
    }

    public List<String> getOriginalInstructions() {
        return new ArrayList<>(originalInstructions);
    }

    public List<String> getOptimizedInstructions() {
        return new ArrayList<>(optimizedInstructions);
    }

    public List<Transformation> getTransformations() {
        return new ArrayList<>(transformations);
    }

    public void printReport() {
        System.out.println("================================================================================");
        System.out.println("               5. ETAPA DE OTIMIZAÇÃO DE CÓDIGO INTERMEDIÁRIO (IR)              ");
        System.out.println("================================================================================");

        System.out.println("\n--- CÓDIGO INTERMEDIÁRIO ANTES DA OTIMIZAÇÃO (" + originalInstructions.size() + " instruções): ---");
        for (String inst : originalInstructions) {
            System.out.println("   " + inst);
        }

        System.out.println("\n--- TRANSFORMAÇÕES REALIZADAS (" + transformations.size() + " alterações): ---");
        if (transformations.isEmpty()) {
            System.out.println("   (Nenhuma transformação necessária - código já ótimo)");
        } else {
            for (Transformation t : transformations) {
                System.out.println(t);
            }
        }

        System.out.println("\n--- CÓDIGO INTERMEDIÁRIO APÓS A OTIMIZAÇÃO (" + optimizedInstructions.size() + " instruções): ---");
        for (String inst : optimizedInstructions) {
            System.out.println("   " + inst);
        }

        int before = originalInstructions.size();
        int after = optimizedInstructions.size();
        int reduced = before - after;
        double pct = before > 0 ? (reduced * 100.0 / before) : 0.0;

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("Instruções Originais : %d%n", before);
        System.out.printf("Instruções Finais    : %d%n", after);
        System.out.printf("Instruções Poupadas  : %d (%.1f%% de redução)%n", reduced, pct);
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
