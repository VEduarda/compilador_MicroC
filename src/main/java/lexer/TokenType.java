package lexer;

/*
 * Enumeração contendo todos os tipos de tokens
 * reconhecidos pelo analisador léxico.
 */
public enum TokenType {

    // Palavras reservadas para tipos
    INT,
    FLOAT,
    VOID,
    CONST,

    // Estruturas de controle
    IF,
    ELSE,
    WHILE,
    BREAK,
    CONTINUE,
    RETURN,

    // Comando de saída
    PRINT,

    // Identificadores (nomes de variáveis e funções)
    ID,

    // Números literais
    NUMBER,

    // Strings literais
    STRING,

    // Operador de atribuição (=)
    ASSIGN,
    
    // Operadores relacionais e igualdade
    EQUAL,       // ==
    NOT_EQUAL,   // !=
    
    // Operadores lógicos
    AND,         // &&
    OR,          // ||
    NOT,         // !
    
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

    // Delimitadores
    COMMA,       // ,
    SEMICOLON,   // ;

    // Final do arquivo
    EOF
}
