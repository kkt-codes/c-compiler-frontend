package com.compiler.exception;

public class LexicalException extends Exception {
    private final int line;
    private final int column;
    private final String invalidLexeme;

    public LexicalException(String message, int line, int column, String invalidLexeme) {
        super(String.format("Lexical Error at Line %d, Col %d: %s ['%s']", line, column, message, invalidLexeme));
        this.line = line;
        this.column = column;
        this.invalidLexeme = invalidLexeme;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getInvalidLexeme() { return invalidLexeme; }
}