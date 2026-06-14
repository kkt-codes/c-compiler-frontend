package com.compiler.semantic;

import com.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    // The active stack for live semantic scope tracking
    private final Stack<Map<String, Symbol>> scopes;
    // The historical ledger for UI presentation
    private final List<Symbol> history;
    private int currentScopeLevel;

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.history = new ArrayList<>();
        this.currentScopeLevel = 0;
        enterScope(); // Push global scope
    }

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

    public boolean declare(String name, TokenType type) {
        Map<String, Symbol> currentScope = scopes.peek();

        if (currentScope.containsKey(name)) {
            return false; // Error: Redeclaration in the same scope
        }

        Symbol newSymbol = new Symbol(name, type, currentScopeLevel);
        currentScope.put(name, newSymbol);

        // NEW: Add to the permanent history ledger for the GUI
        history.add(newSymbol);

        return true;
    }

    public Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    public int getCurrentScopeLevel() {
        return currentScopeLevel;
    }

    // Expose the history ledger to the JavaFX Controller
    public List<Symbol> getHistory() {
        return history;
    }
}