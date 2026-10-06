package main;

import ast.AstJson;
import ast.Nodes.ProgramNode;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.Parser;
import sdt.PostfixTranslator;
import sdt.PrettyPrinter;
import sdt.SDTTestRunner;
import sdt.TACTranslator;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;
import semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponto de entrada do Compilador MicroC com Tradução Dirigida por Sintaxe (SDT).
 *
 * Demonstra a integração completa das fases do compilador:
 *   1. Scanner Léxico (Tokens)
 *   2. Parser Sintático (AST Pura)
 *   3. Analisador Semântico (Tabela de Símbolos e Validação de Tipos)
 *   4. Tradução Dirigida por Sintaxe (SDT - "Uma AST, Três Traduções"):
 *      - Tradutor 1: PostfixTranslator (Notação Polonesa Reversa / RPN)
 *      - Tradutor 2: TACTranslator (Código de Três Endereços - TAC / IR com Melhoria 2: Pool de Temporárias)
 *      - Tradutor 3: PrettyPrinter (Reimpressor Canônico C com Parênteses Explícitos)
 *   5. Execução da Bateria de Testes Obrigatórios (Item 6 do Enunciado)
 */
public class Main {

    public static void main(String[] args) {

        // Garante que acentos e caracteres especiais sejam exibidos corretamente
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));

        System.out.println("================================================================================");
        System.out.println("         COMPILADOR MICROC - FASE DE SÍNTESE E TRADUÇÃO DIRIGIDA POR SINTAXE    ");
        System.out.println("                 \"Uma AST, Múltiplas Traduções: RPN, TAC e Pretty-Printer\"      ");
        System.out.println("================================================================================\n");

        // Código-fonte MicroC demonstrativo semanticamente válido
        String code = """
        int x = 2 + 3 * 4;
        int y = (x + 5) * 2;
        if (y > 20) {
            print(y);
        } else {
            print(0);
        }
        """;

        try {
            System.out.println(">>> CÓDIGO-FONTE MICROC DE ENTRADA:");
            System.out.println(code);

            // =================================================================
            // 1. ANÁLISE LÉXICA
            // =================================================================
            Lexer lexer = new Lexer(code);
            List<Token> tokens = new ArrayList<>();
            Token token;
            long inicioLexico = System.nanoTime();
            do {
                token = lexer.nextToken();
                tokens.add(token);
            } while (token.type != TokenType.EOF);
            long fimLexico = System.nanoTime();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("1. TOKENS ENCONTRADOS (" + tokens.size() + " tokens):");
            for (Token t : tokens) {
                System.out.println("   " + t);
            }

            // =================================================================
            // 2. ANÁLISE SINTÁTICA
            // =================================================================
            long inicioParser = System.nanoTime();
            Parser parser = new Parser(tokens);
            ProgramNode ast = parser.parseProgram();
            long fimParser = System.nanoTime();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("2. AST GERADA (Árvore Sintática Abstrata):");
            ast.print("   ");

            // =================================================================
            // 3. ANÁLISE SEMÂNTICA
            // =================================================================
            long inicioSemantico = System.nanoTime();
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            List<SemanticError> semanticErrors = semanticAnalyzer.analyze(ast);
            long fimSemantico = System.nanoTime();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("3. TABELA DE SÍMBOLOS:");
            System.out.println("   NOME       | TIPO   | CATEGORIA | ESCOPO    | LINHA    | STATUS");
            for (Symbol s : semanticAnalyzer.getSymbolTable().getHistory()) {
                System.out.println("   " + s);
            }

            if (!semanticErrors.isEmpty()) {
                System.out.println("\n[ERROS SEMÂNTICOS ENCONTRADOS]:");
                for (SemanticError e : semanticErrors) {
                    System.out.println("   " + e);
                }
            } else {
                System.out.println("\n   >> Análise semântica concluída sem nenhum erro!");
            }

            // =================================================================
            // 4. TRADUÇÃO DIRIGIDA POR SINTAXE (SDT) - "UMA AST, TRÊS TRADUÇÕES"
            // =================================================================
            System.out.println("\n================================================================================");
            System.out.println("            4. FASE DE SÍNTESE: TRADUÇÃO DIRIGIDA POR SINTAXE (SDT)             ");
            System.out.println("================================================================================");

            // Tradutor 1: PostfixTranslator (RPN)
            long inicioRPN = System.nanoTime();
            PostfixTranslator postfixTranslator = new PostfixTranslator();
            postfixTranslator.translate(ast);
            long fimRPN = System.nanoTime();

            System.out.println("\n--- TRADUTOR 1: NOTAÇÃO POLONESA REVERSA (RPN / PÓS-FIXA) ---");
            postfixTranslator.printRPN();
            System.out.println("Expressões isoladas em RPN: " + String.join(", ", postfixTranslator.getExpressionRPNs()));

            // Tradutor 2: TACTranslator (TAC com Melhoria 2: Pool e Melhoria 3: Constant Folding)
            long inicioTAC = System.nanoTime();
            TACTranslator tacTranslator = new TACTranslator(semanticAnalyzer.getSymbolTable());
            tacTranslator.generate(ast);
            long fimTAC = System.nanoTime();

            System.out.println("\n--- TRADUTOR 2: CÓDIGO DE TRÊS ENDEREÇOS (TAC / IR) ---");
            tacTranslator.printInstructions();

            System.out.println("\n" + tacTranslator.getTempPool().getSummaryReport());

            // Tradutor 3: PrettyPrinter (Código C Canônico)
            long inicioPP = System.nanoTime();
            PrettyPrinter prettyPrinter = new PrettyPrinter();
            prettyPrinter.format(ast);
            long fimPP = System.nanoTime();

            System.out.println("--- TRADUTOR 3: REIMPRESSOR CANÔNICO E LINTER (PRETTY-PRINTER) ---");
            prettyPrinter.printFormatted();

            // Estatísticas do compilador
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("ESTATÍSTICAS GERAIS DE COMPILAÇÃO");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("Total de Tokens                 : %d%n", tokens.size());
            System.out.printf("Tempo Léxico                    : %.4f ms%n", (fimLexico - inicioLexico) / 1_000_000.0);
            System.out.printf("Tempo Sintático                 : %.4f ms%n", (fimParser - inicioParser) / 1_000_000.0);
            System.out.printf("Tempo Semântico                 : %.4f ms%n", (fimSemantico - inicioSemantico) / 1_000_000.0);
            System.out.printf("Tempo Tradução RPN              : %.4f ms%n", (fimRPN - inicioRPN) / 1_000_000.0);
            System.out.printf("Tempo Tradução TAC              : %.4f ms%n", (fimTAC - inicioTAC) / 1_000_000.0);
            System.out.printf("Tempo Pretty-Printing           : %.4f ms%n", (fimPP - inicioPP) / 1_000_000.0);
            System.out.printf("Total de Instruções TAC         : %d%n", tacTranslator.getInstructions().size());
            System.out.printf("Erros Semânticos                : %d%n", semanticErrors.size());
            System.out.println("Status                          : COMPILAÇÃO E TRADUÇÃO CONCLUÍDAS COM SUCESSO!\n");

            // =================================================================
            // 5. BATERIA DE TESTES OBRIGATÓRIOS DO ENUNCIADO (ITEM 6)
            // =================================================================
            SDTTestRunner.runAllTests();

        } catch (Exception e) {
            System.err.println("ERRO DURANTE A COMPILAÇÃO:");
            e.printStackTrace();
        }
    }
}