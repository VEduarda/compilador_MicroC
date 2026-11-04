package test.unit;

import backend.ExecutionResult;
import backend.TACInterpreter;
import test.common.TestFramework;

import java.util.List;

public final class TacInterpreterUnitTests {

    private TacInterpreterUnitTests() {
    }

    public static void register(TestFramework tests) {
        tests.add("Interpretador executa operações aritméticas e print", () -> {
            ExecutionResult result = new TACInterpreter(false).execute(List.of(
                    "left = 7",
                    "right = 5",
                    "sum = left + right",
                    "product = sum * 2",
                    "print product"));
            TestFramework.assertEquals(List.of("24"), result.getOutputLines(), "Saída calculada");
            TestFramework.assertEquals(24, result.getVariable("product"), "Valor final da variável");
        });
    }
}
