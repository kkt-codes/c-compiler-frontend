package com.compiler.lexer;

public enum TokenType {
    // Data Types
    KEYWORD_INT,
    KEYWORD_FLOAT,
    KEYWORD_CHAR,
    KEYWORD_VOID,

    // Control Flow Keywords
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_WHILE,
    KEYWORD_FOR,
    KEYWORD_RETURN,

    // Identifiers & Literals
    IDENTIFIER,
    LITERAL_NUM,
    LITERAL_CHAR,

    // Operators
    ASSIGN,       // =
    PLUS,         // +
    MINUS,        // -
    MULT,         // *
    DIV,          // /

    // Relational Operators
    EQ,           // ==
    NEQ,          // !=
    LT,           // <
    GT,           // >
    LTE,          // <=
    GTE,          // >=

    // Delimiters & Punctuation
    SEMICOLON,    // ;
    COMMA,        // ,
    LPAREN,       // (
    RPAREN,       // )
    LBRACE,       // {
    RBRACE,       // }

    // Special Tokens
    EOF,          // End of File
    UNKNOWN       // Graceful Lexical Error Handling
}