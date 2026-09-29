package lexer;

/*
 * Enumeração contendo todos os tipos de tokens
 * reconhecidos pelo analisador léxico.
 */
public enum TokenType {

    // Palavra reservada para declaração de inteiro
    INT,

    // Estruturas de controle
    IF,
    ELSE,
    WHILE,

    // Comando de saída
    PRINT,

    // Identificadores (nomes de variáveis)
    ID,

    // Números literais
    NUMBER,

    // Strings literais
    STRING,

    // Operador de atribuição (=)
    ASSIGN,
    
    // Operadores relacionais
    EQUAL,       // ==
    NOT_EQUAL,   // !=
    
    // Operadores relacionais
    AND,
    OR,
    NOT,
    
    // Operadores aritméticos
    PLUS,        // +
    MINUS,       // -
    MULT,        // *
    DIV,         // /

    // Comparações
    LT,          // <
    GT,          // >
    LTE,         // <=
    GTE,         // >=

    // Parênteses
    LPAREN,
    RPAREN,

    // Chaves
    LBRACE,
    RBRACE,

    // Final de comando
    SEMICOLON,

    // Final do arquivo
    EOF
}