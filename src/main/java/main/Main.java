package main;

import ast.AstJson;
import ast.Nodes.ProgramNode;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;
import semantic.Symbol;

/*
 * Classe principal do compilador.
 *
 * Responsável por:
 * 1. Receber o código-fonte.
 * 2. Executar a análise léxica.
 * 3. Executar a análise sintática.
 * 4. Gerar e exibir a AST.
 * 5. Mostrar estatísticas de execução.
 */
public class Main {

    public static void main(String[] args) {

        // Garante que acentos (ç, ã, é...) sejam exibidos corretamente
        // no console, independente da codificação padrão do sistema.
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));

        // Código-fonte que será compilado
        /*String code = """
            int x = 10;
            print(x);
            """;
        */
        
        // Programa de demonstração: propositalmente contém alguns
        // erros semânticos para mostrar o analisador em ação
        // (redeclaração, tipo incompatível, variável não declarada
        // e sombreamento válido entre escopos).
        String code = """
        int x = 10;
        int y = "texto";
        int x = 5;

        if (x > 5) {
            int x = 99;
            print(x);
        }

        print(x);
        z = 1;
        """;

        try {

            // Cria o analisador léxico
            Lexer lexer = new Lexer(code);

            List<Token> tokens = new ArrayList<>();

            Token token;

            // Marca início da análise léxica
            long inicioLexico = System.nanoTime();

            // Gera tokens até encontrar EOF
            do {

                token = lexer.nextToken();

                tokens.add(token);

            } while (token.type != TokenType.EOF);

            long fimLexico = System.nanoTime();

            System.out.println("TOKENS ENCONTRADOS");

            // Exibe todos os tokens gerados
            for (Token t : tokens) {
                System.out.println(t);
            }

            // Marca início da análise sintática
            long inicioParser = System.nanoTime();

            Parser parser = new Parser(tokens);

            // Constrói a AST
            ProgramNode ast = parser.parseProgram();

            long fimParser = System.nanoTime();
            
            System.out.println("\n");
            System.out.println("AST GERADA");

            // Exibe árvore sintática
            ast.print();
            
            
            System.out.println("\n");
            System.out.println("AST EM JSON");
            System.out.println(
                    AstJson.toJson(ast));

            // Marca início da análise semântica
            long inicioSemantico = System.nanoTime();

            SemanticAnalyzer semanticAnalyzer =
                    new SemanticAnalyzer();

            List<SemanticError> semanticErrors =
                    semanticAnalyzer.analyze(ast);

            long fimSemantico = System.nanoTime();

            System.out.println("\n");
            System.out.println("TABELA DE SÍMBOLOS");
            System.out.println(
                    "NOME       | TIPO   | CATEGORIA | ESCOPO    | LINHA    | STATUS");

            for (Symbol s : semanticAnalyzer.getSymbolTable().getHistory()) {
                System.out.println(s);
            }

            System.out.println("\n");
            System.out.println("ERROS SEMÂNTICOS");

            if (semanticErrors.isEmpty()) {
                System.out.println(
                        "Nenhum erro semântico encontrado.");
            } else {
                for (SemanticError e : semanticErrors) {
                    System.out.println(e);
                }
            }

            // Estatísticas do compilador
            System.out.println("\n");
            System.out.println("ESTATÍSTICAS");

            System.out.printf(
                    "Total de Tokens: %d%n",
                    tokens.size());

            System.out.printf(
                    "Tempo Léxico: %.4f ms%n",
                    (fimLexico - inicioLexico)
                            / 1_000_000.0);

            System.out.printf(
                    "Tempo Sintático: %.4f ms%n",
                    (fimParser - inicioParser)
                            / 1_000_000.0);

            System.out.printf(
                    "Tempo Semântico: %.4f ms%n",
                    (fimSemantico - inicioSemantico)
                            / 1_000_000.0);

            System.out.printf(
                    "Total de Erros Semânticos: %d%n",
                    semanticErrors.size());

            if (semanticErrors.isEmpty()) {
                System.out.println(
                        "COMPILAÇÃO FINALIZADA COM SUCESSO");
            } else {
                System.out.println(
                        "COMPILAÇÃO FINALIZADA COM ERROS SEMÂNTICOS");
            }
        }
        catch (Exception e) {

            // Exibe erro encontrado
            System.err.println("ERRO ENCONTRADO");
            System.err.println(e.getMessage());
        }
    }
}