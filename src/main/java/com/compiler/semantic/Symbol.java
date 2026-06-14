package com.compiler.semantic;

import com.compiler.lexer.TokenType;

public class Symbol {
    private final String name;
    private final TokenType type; // e.g., KEYWORD_INT, KEYWORD_FLOAT
    private final int scopeLevel;

    public Symbol(String name, TokenType type, int scopeLevel) {
        this.name = name;
        this.type = type;
        this.scopeLevel = scopeLevel;
    }

    public String getName() { return name; }
    public TokenType getType() { return type; }
    public int getScopeLevel() { return scopeLevel; }

    @Override
    public String toString() {
        return String.format("Symbol[Name: %s, Type: %s, Scope: %d]", name, type, scopeLevel);
    }
}