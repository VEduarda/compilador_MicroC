package test.unit;

import optimizer.OptimizationReport;
import optimizer.TACOptimizer;
import test.common.TestFramework;

import java.util.List;

public final class OptimizerUnitTests {

    private OptimizerUnitTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Otimizador simplifica a + 0", () -> {
            TestFramework.assertEquals(List.of("result = a"), optimize("result = a + 0"),
                    "Resultado da identidade a + 0");
        });

        tests.add("Otimizador simplifica 0 + b", () -> {
            TestFramework.assertEquals(List.of("result = b"), optimize("result = 0 + b"),
                    "Resultado da identidade 0 + b");
        });

        tests.add("Otimizador simplifica c * 1", () -> {
            TestFramework.assertEquals(List.of("result = c"), optimize("result = c * 1"),
                    "Resultado da identidade c * 1");
        });

        tests.add("Otimizador simplifica d * 0", () -> {
            TestFramework.assertEquals(List.of("result = 0"), optimize("result = d * 0"),
                    "Resultado da identidade d * 0");
        });

        tests.add("Otimizador simplifica e - 0", () -> {
            TestFramework.assertEquals(List.of("result = e"), optimize("result = e - 0"),
                    "Resultado da identidade e - 0");
        });

        tests.add("Otimizador simplifica f / 1", () -> {
            TestFramework.assertEquals(List.of("result = f"), optimize("result = f / 1"),
                    "Resultado da identidade f / 1");
        });

        tests.add("Otimizador elimina subexpressões comuns", () -> {
            TACOptimizer optimizer = new TACOptimizer();
            List<String> result = optimizer.optimize(List.of(
                    "t1 = left + right",
                    "t2 = left + right",
                    "print t2"));
            long additions = result.stream().filter(line -> line.contains("left + right")).count();
            boolean cseRecorded = optimizer.getReport().getTransformations().stream()
                    .map(transformation -> transformation.technique)
                    .anyMatch("Common Subexpression Elimination"::equals);
            TestFramework.assertEquals(1L, additions, "Deve permanecer um único cálculo da soma");
            TestFramework.assertTrue(cseRecorded, "Relatório deve identificar a transformação CSE");
        });
    }

    private static List<String> optimize(String instruction) {
        return new TACOptimizer().optimize(List.of(instruction));
    }
}
