package test;

import test.common.TestFramework;
import test.integration.CompilerPipelineIntegrationTests;
import test.unit.AstParserUnitTests;
import test.unit.LexerUnitTests;
import test.unit.OptimizerUnitTests;
import test.unit.SemanticUnitTests;
import test.unit.TacInterpreterUnitTests;

public final class TestSuiteRunner {

    private TestSuiteRunner() {
    }

    public static void main(String[] args) {
        TestFramework tests = new TestFramework();
        LexerUnitTests.register(tests);
        AstParserUnitTests.register(tests);
        SemanticUnitTests.register(tests);
        TacInterpreterUnitTests.register(tests);
        OptimizerUnitTests.register(tests);
        CompilerPipelineIntegrationTests.register(tests);

        TestFramework.Result result = tests.run();
        if (!result.successful()) {
            System.exit(1);
        }
    }
}
