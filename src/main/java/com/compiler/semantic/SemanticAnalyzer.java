package com.compiler.semantic;

import com.compiler.exception.SemanticException;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;
import com.compiler.parser.ParseTreeNode;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final ParseTreeNode root;
    private final SymbolTable symbolTable;
    private final List<SemanticException> errorLog;

    public SemanticAnalyzer(ParseTreeNode root) {
        this.root = root;
        this.symbolTable = new SymbolTable();
        this.errorLog = new ArrayList<>();
    }

    public void analyze() {
        visit(root);
    }

    public List<SemanticException> getErrorLog() {
        return errorLog;
    }

    public List<Symbol> getSymbolHistory() {
        return symbolTable.getHistory();
    }

    private void visit(ParseTreeNode node) {
        if (node == null) return;
        if (node.getToken() != null || node.getNodeName().equals("ε") || node.getNodeName().equals("蔚")) {
            return;
        }

        switch (node.getNodeName()) {
            case "CompoundStatement" -> visitCompoundStatement(node);
            case "Declaration" -> visitDeclaration(node);
            case "IdStatement" -> visitIdStatement(node);
            case "IterativeStatement" -> visitIterativeStatement(node);
            case "SelectionStatement" -> visitSelectionStatement(node);
            default -> {
                for (ParseTreeNode child : node.getChildren()) {
                    visit(child);
                }
            }
        }
    }

    private void visitCompoundStatement(ParseTreeNode node) {
        symbolTable.enterScope();
        for (ParseTreeNode child : node.getChildren()) {
            visit(child);
        }
        symbolTable.exitScope();
    }

    private void visitDeclaration(ParseTreeNode node) {
        TokenType declaredType = null;
        Token identifierToken = null;

        for (ParseTreeNode child : node.getChildren()) {
            if (child.getToken() != null) {
                Token t = child.getToken();
                if (t.getType() == TokenType.KEYWORD_INT || t.getType() == TokenType.KEYWORD_FLOAT ||
                        t.getType() == TokenType.KEYWORD_CHAR || t.getType() == TokenType.KEYWORD_VOID) {
                    declaredType = t.getType();
                } else if (t.getType() == TokenType.IDENTIFIER) {
                    identifierToken = t;
                }
            } else {
                if (child.getNodeName().equals("Type")) {
                    for (ParseTreeNode typeChild : child.getChildren()) {
                        if (typeChild.getToken() != null) declaredType = typeChild.getToken().getType();
                    }
                }
                visit(child);
            }
        }

        if (identifierToken != null && declaredType != null) {
            boolean success = symbolTable.declare(identifierToken.getLexeme(), declaredType);
            if (!success) {
                logError(identifierToken, "Variable '" + identifierToken.getLexeme() + "' already declared in this scope.");
            }
        }
    }

    private void visitIdStatement(ParseTreeNode node) {
        Token identifierToken = null;
        ParseTreeNode expressionNode = null;

        for (ParseTreeNode child : node.getChildren()) {
            if (child.getToken() != null && child.getToken().getType() == TokenType.IDENTIFIER) {
                identifierToken = child.getToken();
            }
            if (child.getNodeName().contains("Expression") || child.getNodeName().contains("Expr")) {
                expressionNode = child;
            }
            visit(child);
        }

        if (identifierToken != null) {
            Symbol sym = symbolTable.lookup(identifierToken.getLexeme());
            if (sym == null) {
                logError(identifierToken, "Variable '" + identifierToken.getLexeme() + "' is undeclared.");
            } else if (expressionNode != null) {
                TokenType derivedType = getExpressionType(expressionNode);
                if (derivedType != TokenType.UNKNOWN && sym.getType() != derivedType) {
                    if (!isCompatible(sym.getType(), derivedType)) {
                        logError(identifierToken, "Type mismatch: Cannot assign " + cleanTypeName(derivedType) + " to " + cleanTypeName(sym.getType()) + ".");
                    }
                }
            }
        }
    }

    private void visitIterativeStatement(ParseTreeNode node) {
        for (ParseTreeNode child : node.getChildren()) {
            if (child.getNodeName().contains("Expression") || child.getNodeName().contains("Expr")) {
                getExpressionType(child);
            }
            visit(child);
        }
    }

    private void visitSelectionStatement(ParseTreeNode node) {
        for (ParseTreeNode child : node.getChildren()) {
            if (child.getNodeName().contains("Expression") || child.getNodeName().contains("Expr")) {
                getExpressionType(child);
            }
            visit(child);
        }
    }

    private TokenType getExpressionType(ParseTreeNode node) {
        if (node == null) return null;

        if (node.getToken() != null) {
            Token token = node.getToken();
            if (token.getType() == TokenType.IDENTIFIER) {
                Symbol s = symbolTable.lookup(token.getLexeme());
                if (s == null) {
                    logError(token, "Variable '" + token.getLexeme() + "' is undeclared.");
                    return TokenType.UNKNOWN;
                }
                return s.getType();
            }
            if (token.getType() == TokenType.LITERAL_NUM) {
                return token.getLexeme().contains(".") ? TokenType.KEYWORD_FLOAT : TokenType.KEYWORD_INT;
            }
            if (token.getType() == TokenType.LITERAL_CHAR) {
                return TokenType.KEYWORD_CHAR;
            }
            return token.getType();
        }

        // Handle relational expression operations mapping
        if (node.getNodeName().equals("Expression") || node.getNodeName().equals("SimpleExpression")) {
            boolean holdsRelationalOp = false;
            for (ParseTreeNode child : node.getChildren()) {
                if (child.getToken() != null) {
                    TokenType t = child.getToken().getType();
                    if (t == TokenType.EQ || t == TokenType.NEQ || t == TokenType.LT ||
                            t == TokenType.GT || t == TokenType.LTE || t == TokenType.GTE) {
                        holdsRelationalOp = true;
                    }
                }
            }
            if (holdsRelationalOp) {
                ParseTreeNode left = node.getChildren().size() > 0 ? node.getChildren().get(0) : null;
                ParseTreeNode right = node.getChildren().size() > 2 ? node.getChildren().get(2) : null;
                TokenType leftType = getExpressionType(left);
                TokenType rightType = getExpressionType(right);
                if (leftType == TokenType.UNKNOWN || rightType == TokenType.UNKNOWN || leftType != rightType) {
                    return TokenType.UNKNOWN;
                }
                return TokenType.KEYWORD_INT; // Relational checks evaluate to integer levels
            }
        }

        TokenType structuralType = null;
        for (ParseTreeNode child : node.getChildren()) {
            TokenType childType = getExpressionType(child);
            if (childType != null) {
                if (childType == TokenType.UNKNOWN) return TokenType.UNKNOWN;
                if (structuralType == null) {
                    structuralType = childType;
                } else if (structuralType != childType) {
                    if ((structuralType == TokenType.KEYWORD_INT && childType == TokenType.KEYWORD_FLOAT) ||
                            (structuralType == TokenType.KEYWORD_FLOAT && childType == TokenType.KEYWORD_INT)) {
                        structuralType = TokenType.KEYWORD_FLOAT;
                    } else {
                        return TokenType.UNKNOWN;
                    }
                }
            }
        }
        return structuralType;
    }

    private boolean isCompatible(TokenType target, TokenType evaluated) {
        if (target == evaluated) return true;
        return (target == TokenType.KEYWORD_FLOAT && evaluated == TokenType.KEYWORD_INT);
    }

    private void logError(Token token, String message) {
        errorLog.add(new SemanticException(message, token.getLine(), token.getColumn()));
    }

    private String cleanTypeName(TokenType type) {
        return switch (type) {
            case KEYWORD_INT -> "int";
            case KEYWORD_FLOAT -> "float";
            case KEYWORD_CHAR -> "char";
            case KEYWORD_VOID -> "void";
            default -> type.name();
        };
    }
}