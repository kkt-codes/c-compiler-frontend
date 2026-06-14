package com.compiler.exception;

public class SemanticException extends Exception {
    private final int line;
    private final int column;

    public SemanticException(String message, int line, int column) {
        super(String.format("Semantic Error at Line %d, Col %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
}