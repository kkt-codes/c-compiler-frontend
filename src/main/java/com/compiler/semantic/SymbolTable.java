package com.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    // A stack of scopes. Each scope is a map of variable names to their Symbol data.
    private final Stack<Map<String, Symbol>> scopes;
    private int currentScopeLevel;

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.currentScopeLevel = 0;
        // Push the global scope immediately
        enterScope();
    }

    // --- Scope Management ---

    public void enterScope() {
        scopes.push(new HashMap<>());
        currentScopeLevel++;
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
            currentScopeLevel--;
        } else {
            throw new RuntimeException("Compiler Error: Cannot exit global scope.");
        }
    }

    // --- Symbol Operations ---

    /**
     * Declares a new variable in the CURRENT scope.
     * Returns true if successful, false if the variable already exists in this specific scope.
     */
    public boolean declare(String name, com.compiler.lexer.TokenType type) {
        Map<String, Symbol> currentScope = scopes.peek();

        if (currentScope.containsKey(name)) {
            return false; // Error: Variable already declared in this scope!
        }

        Symbol newSymbol = new Symbol(name, type, currentScopeLevel);
        currentScope.put(name, newSymbol);
        return true;
    }

    /**
     * Looks up a variable starting from the local scope and moving up to global.
     * Returns the Symbol if found, or null if it was never declared.
     */
    public Symbol lookup(String name) {
        // Search from top of stack (local) down to bottom (global)
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // Variable not found
    }

    public int getCurrentScopeLevel() {
        return currentScopeLevel;
    }
}