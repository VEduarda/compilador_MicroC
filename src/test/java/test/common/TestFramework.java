package test.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TestFramework {

    private final List<TestCase> tests = new ArrayList<>();
    private int passed;
    private int failed;
    private int pending;

    public void add(String name, Runnable body) {
        tests.add(new TestCase(name, body, null));
    }

    public void pending(String name, String reason) {
        tests.add(new TestCase(name, null, reason));
    }

    public Result run() {
        for (TestCase test : tests) {
            if (test.pendingReason != null) {
                pending++;
                System.out.printf("[PENDENTE] %s — %s%n", test.name, test.pendingReason);
                continue;
            }

            try {
                test.body.run();
                passed++;
                System.out.println("[OK] " + test.name);
            } catch (Throwable failure) {
                failed++;
                System.out.printf("[FALHA] %s — %s%n", test.name, failure);
            }
        }

        System.out.printf("%nResultado: %d executados, %d aprovados, %d reprovados, %d pendentes.%n",
                passed + failed, passed, failed, pending);
        return new Result(passed, failed, pending);
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (esperado: " + expected + ", obtido: " + actual + ")");
        }
    }

    public static <T extends Throwable> T assertThrows(
            Class<T> expectedType, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expectedType.isInstance(failure)) {
                return expectedType.cast(failure);
            }
            throw new AssertionError(message + " (tipo lançado: " + failure.getClass().getName() + ")", failure);
        }
        throw new AssertionError(message + " (nenhuma exceção lançada)");
    }

    public record Result(int passed, int failed, int pending) {
        public boolean successful() {
            return failed == 0;
        }
    }

    private record TestCase(String name, Runnable body, String pendingReason) {
    }
}