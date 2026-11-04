package main;

import backend.CompilerPipeline;
import backend.CompilerPipeline.CompilationResult;
import backend.ExecutionResult;
import semantic.SemanticError;

import java.util.List;

/**
 * Bateria de Testes Automatizados para validação das pendências implementadas:
 *   - Pendência 3: 'const' no lexer, parser, AST, tabela de símbolos e bloqueio de reatribuição.
 *   - Pendência 4: AST, tradução TAC e execução de chamadas de função, aridade, tipos de parâmetros e chamadas/retornos recursivos.
 *   - Pendência 5: Validação do 'return' conforme tipo da função e posição (dentro/fora de funções).
 */
public class FeatureValidationRunner {

    private static int totalChecks = 0;
    private static int failedChecks = 0;

    public static void check(boolean condition, String description) {
        totalChecks++;
        if (condition) {
            System.out.println("  [OK] " + description);
        } else {
            failedChecks++;
            System.out.println("  [FALHA] " + description);
        }
    }

    public static void checkSuccess(CompilationResult res, String description) {
        if (!res.success) {
            System.out.println("  [DEBUG ERRO] Stage: " + res.failedStage + " | Msg: " + res.errorMessage);
            if (res.semanticErrors != null) {
                for (SemanticError err : res.semanticErrors) {
                    System.out.println("    -> " + err);
                }
            }
        }
        check(res.success, description);
    }

    public static int getTotalChecks() {
        return totalChecks;
    }

    public static int getFailedChecks() {
        return failedChecks;
    }

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        runAllValidationTests();
    }

    public static void runAllValidationTests() {
        totalChecks = 0;
        failedChecks = 0;

        System.out.println("================================================================================");
        System.out.println(" BATERIA DE TESTES: PENDÊNCIAS 3 (CONST), 4 (FUNCTION CALLS) E 5 (RETURN CHECK) ");
        System.out.println("================================================================================\n");

        CompilerPipeline pipeline = new CompilerPipeline();

        // =====================================================================
        // ITEM 3: CONST
        // =====================================================================
        System.out.println(">>> GRUPO 1: PENDÊNCIA 3 - SUPORTE E RESTRIÇÕES DE 'const'");

        // 1.1 Declaração válida de const e uso em expressão
        {
            String code = """
            const int MAX = 100;
            int main() {
                int y = MAX + 5;
                return y;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ConstValido");
            check(res.success, "Declaração e uso válido de 'const' compila com sucesso");
            ExecutionResult exec = res.executionResult;
            check(exec.getReturnValue() == 105, "Execução com 'const' calculou 100 + 5 = 105");
        }

        // 1.2 Reatribuição de constante deve gerar erro semântico R9-ReatribuicaoConstante
        {
            String code = """
            const int X = 10;
            int main() {
                X = 20;
                return X;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ConstReassign");
            check(!res.success, "Reatribuição de constante aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R9-ReatribuicaoConstante"),
                    "Erro R9-ReatribuicaoConstante detectado para reatribuição de variável const");
        }

        // 1.3 Constante não inicializada deve gerar erro semântico R9-ConstanteNaoInicializada
        {
            String code = """
            const int Y;
            int main() {
                return 0;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ConstUninitialized");
            check(!res.success, "Constante não inicializada aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R9-ConstanteNaoInicializada"),
                    "Erro R9-ConstanteNaoInicializada detectado para declaração const sem valor");
        }

        // 1.4 Parâmetro const em função aceito e executado
        {
            String code = """
            int addFive(const int val) {
                return val + 5;
            }
            int main() {
                return addFive(20);
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ConstParamValid");
            checkSuccess(res, "Função com parâmetro 'const' compila com sucesso");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 25, "Execução com parâmetro const calculou 20 + 5 = 25");
        }

        // 1.5 Reatribuição de parâmetro const deve gerar erro R9-ReatribuicaoConstante
        {
            String code = """
            int mutate(const int val) {
                val = 42;
                return val;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ConstParamReassign");
            check(!res.success, "Reatribuição de parâmetro const aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R9-ReatribuicaoConstante"),
                    "Erro R9-ReatribuicaoConstante detectado para modificação de parâmetro const");
        }

        // =====================================================================
        // ITEM 4: CHAMADAS DE FUNÇÃO (AST, TAC, EXECUÇÃO E REGRAS SEMÂNTICAS)
        // =====================================================================
        System.out.println("\n>>> GRUPO 2: PENDÊNCIA 4 - CHAMADAS DE FUNÇÃO, TAC, EXECUÇÃO E VERIFICAÇÕES");

        // 2.1 Chamada de função void como comando (statement)
        {
            String code = """
            int log = 0;
            void trigger() {
                log = 77;
            }
            int main() {
                trigger();
                return log;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "VoidCallStatement");
            checkSuccess(res, "Chamada de função void como comando compila");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 77, "Execução de chamada void como comando produziu efeito colateral correto (log = 77)");
        }

        // 2.2 Chamada de função com retorno em expressão aritmética
        {
            String code = """
            int mul(int a, int b) {
                return a * b;
            }
            int main() {
                int res = mul(6, 7) + 8;
                return res;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "FuncCallExpression");
            checkSuccess(res, "Chamada de função dentro de expressão compila");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 50, "Execução calculou mul(6, 7) + 8 = 42 + 8 = 50");
        }

        // 2.3 Chamada de função recursiva (Fatorial)
        {
            String code = """
            int fact(int n) {
                if (n <= 1) {
                    return 1;
                }
                return n * fact(n - 1);
            }
            int main() {
                return fact(5);
            }
            """;
            CompilationResult res = pipeline.compileString(code, "RecursiveFact");
            checkSuccess(res, "Função recursiva com chamada aninhada compila");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 120, "Execução recursiva do fatorial de 5 resultou exatamente em 120");
        }

        // 2.4 Funções interagindo com variáveis globais
        {
            String code = """
            int counter = 10;
            void incBy(int amount) {
                counter = counter + amount;
            }
            int main() {
                incBy(5);
                incBy(15);
                return counter;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "GlobalInteraction");
            checkSuccess(res, "Múltiplas chamadas de função interagindo com variável global compilam");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 30, "Execução propagou modificações da variável global (10 + 5 + 15 = 30)");
        }

        // 2.5 Erro: Chamada de função não declarada (R1-FuncaoNaoDeclarada)
        {
            String code = """
            int main() {
                foo(1, 2);
                return 0;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "UndeclaredFunction");
            check(!res.success, "Chamada de função não declarada aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R1-FuncaoNaoDeclarada"),
                    "Erro R1-FuncaoNaoDeclarada detectado ao invocar função inexistente");
        }

        // 2.6 Erro: Chamada de identificador que não é função (R1-NaoEFuncao)
        {
            String code = """
            int x = 10;
            int main() {
                x(5);
                return 0;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "CallNonFunction");
            check(!res.success, "Chamada de variável como função aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R1-NaoEFuncao"),
                    "Erro R1-NaoEFuncao detectado ao tentar invocar variável 'x'");
        }

        // 2.7 Erro: Aridade incorreta - menos argumentos (R10-AridadeIncorreta)
        {
            String code = """
            int calc(int a, int b) {
                return a + b;
            }
            int main() {
                return calc(10);
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ArityTooFew");
            check(!res.success, "Passagem de menos argumentos do que os parâmetros aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R10-AridadeIncorreta"),
                    "Erro R10-AridadeIncorreta detectado quando passados menos argumentos");
        }

        // 2.8 Erro: Aridade incorreta - mais argumentos (R10-AridadeIncorreta)
        {
            String code = """
            int calc(int a, int b) {
                return a + b;
            }
            int main() {
                return calc(10, 20, 30);
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ArityTooMany");
            check(!res.success, "Passagem de mais argumentos do que os parâmetros aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R10-AridadeIncorreta"),
                    "Erro R10-AridadeIncorreta detectado quando passados argumentos a mais");
        }

        // 2.9 Erro: Tipo incompatível de argumento (R3-TipoIncompativel)
        {
            String code = """
            int doubleVal(int x) {
                return x * 2;
            }
            int main() {
                return doubleVal("texto");
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ArgTypeMismatch");
            check(!res.success, "Passagem de tipo incompatível de argumento aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R3-TipoIncompativel"),
                    "Erro R3-TipoIncompativel detectado ao passar string para parâmetro int");
        }

        // 2.10 Erro: Função void usada em expressão (R11-FuncaoVoidEmExpressao)
        {
            String code = """
            void doNothing() {}
            int main() {
                int a = doNothing() + 1;
                return a;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "VoidInExpr");
            check(!res.success, "Função void em expressão aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R11-FuncaoVoidEmExpressao"),
                    "Erro R11-FuncaoVoidEmExpressao detectado ao usar retorno void em expressão");
        }

        // =====================================================================
        // ITEM 5: VALIDAÇÃO DE RETURN
        // =====================================================================
        System.out.println("\n>>> GRUPO 3: PENDÊNCIA 5 - VALIDAÇÃO DE POSIÇÃO E TIPO DE 'return'");

        // 3.1 Erro: Return fora de função (R12-ReturnForaDeFuncao)
        {
            String code = """
            int a = 10;
            return a;
            """;
            CompilationResult res = pipeline.compileString(code, "ReturnOutsideFunction");
            check(!res.success, "Return no nível global aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R12-ReturnForaDeFuncao"),
                    "Erro R12-ReturnForaDeFuncao detectado para comando return fora de função");
        }

        // 3.2 Erro: Função void retornando valor (R13-RetornoIncompativel)
        {
            String code = """
            void doAction() {
                return 42;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "VoidReturningValue");
            check(!res.success, "Função void com return de valor aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R13-RetornoIncompativel"),
                    "Erro R13-RetornoIncompativel detectado para função void retornando expressão");
        }

        // 3.3 Erro: Função não-void com return vazio (R13-RetornoIncompativel)
        {
            String code = """
            int getValue() {
                return;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "NonVoidEmptyReturn");
            check(!res.success, "Função int com return vazio aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R13-RetornoIncompativel"),
                    "Erro R13-RetornoIncompativel detectado para função int sem expressão de retorno");
        }

        // 3.4 Erro: Função não-void retornando tipo incompatível (R13-RetornoIncompativel)
        {
            String code = """
            int getNumber() {
                return "incompativel";
            }
            """;
            CompilationResult res = pipeline.compileString(code, "ReturnTypeMismatch");
            check(!res.success, "Função int retornando string aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R13-RetornoIncompativel"),
                    "Erro R13-RetornoIncompativel detectado ao retornar string em função int");
        }

        // 3.5 Erro: Função não-void sem comando return (R14-FuncaoSemRetorno)
        {
            String code = """
            int noReturn() {
                int x = 10;
                x = x + 1;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "MissingReturn");
            check(!res.success, "Função int sem comando return aborta compilação");
            check(hasErrorCode(res.semanticErrors, "R14-FuncaoSemRetorno"),
                    "Erro R14-FuncaoSemRetorno detectado em função int sem nenhum comando return");
        }

        // 3.6 Válido: Função void com return vazio explícito
        {
            String code = """
            void earlyExit(int flag) {
                if (flag == 1) {
                    return;
                }
                print(flag);
            }
            int main() {
                earlyExit(1);
                return 0;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "VoidEmptyReturnValid");
            checkSuccess(res, "Função void com comando return vazio compila com sucesso");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 0, "Execução encerrou corretamente através de return em função void");
        }

        // 3.7 Válido: Função void sem comando return (retorno implícito gerado pelo TAC)
        {
            String code = """
            int sideEffect = 0;
            void perform() {
                sideEffect = 99;
            }
            int main() {
                perform();
                return sideEffect;
            }
            """;
            CompilationResult res = pipeline.compileString(code, "VoidImplicitReturnValid");
            checkSuccess(res, "Função void sem return explícito aceita sem erros");
            ExecutionResult exec = res.executionResult;
            check(exec != null && exec.getReturnValue() == 99, "Retorno implícito da função void retornou o controle normalmente");
        }

        System.out.println("\n================================================================================");
        System.out.println(" TOTAL DE CHECKS: " + totalChecks + " | SUCESSOS: " + (totalChecks - failedChecks) 
                + " | FALHAS: " + failedChecks);
        System.out.println("================================================================================\n");
    }

    private static boolean hasErrorCode(List<SemanticError> errors, String ruleCode) {
        if (errors == null) return false;
        for (SemanticError err : errors) {
            if (err.rule != null && err.rule.contains(ruleCode)) {
                return true;
            }
            if (err.message != null && err.message.contains(ruleCode)) {
                return true;
            }
        }
        return false;
    }
}
