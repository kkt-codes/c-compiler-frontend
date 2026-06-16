package com.compiler.semantic;

public class Symbol {
    private final String name;
    private final String type;       // int, double, void, etc.
    private final String category;   // Variable, Function, Parameter
    private final String scope;      // Global, Local
    private final int lineNumber;

    public Symbol(String name, String type, String category, String scope, int lineNumber) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.scope = scope;
        this.lineNumber = lineNumber;
    }

    // Getters matching standard Java Bean naming conventions for TableView binding
    public String getName() { return name; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getScope() { return scope; }
    public int getLineNumber() { return lineNumber; }
}