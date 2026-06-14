package com.compiler.semantic;

import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;
import com.compiler.parser.ParseTreeNode;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final ParseTreeNode root;
    private final SymbolTable symbolTable;
    private final List<String> semanticErrors;

    public SemanticAnalyzer(ParseTreeNode root) {
        this.root = root;
        this.symbolTable = new SymbolTable();
        this.semanticErrors = new ArrayList<>();
    }

    public void analyze() {
        visit(root);
    }

    public List<String> getSemanticErrors() {
        return semanticErrors;
    }

    // --- Core Tree Traversal ---

    private void visit(ParseTreeNode node) {
        if (node == null) return;

        // Leaf nodes or Epsilon branches don't need statement-level routing
        if (node.getToken() != null || node.getNodeName().equals("ε")) {
            return;
        }

        switch (node.getNodeName()) {
            case "CompoundStatement":
                visitCompoundStatement(node);
                break;
            case "Declaration":
                visitDeclaration(node);
                break;
            case "IdStatement":
                visitIdStatement(node);
                break;
            case "IterativeStatement":
                visitIterativeStatement(node);
                break;
            case "SelectionStatement":
                visitSelectionStatement(node);
                break;
            case "ReturnStatement":
                visitReturnStatement(node);
                break;
            default:
                // Cascade down through intermediate structural nodes (Program', StatementList, etc.)
                for (ParseTreeNode child : node.getChildren()) {
                    visit(child);
                }
                break;
        }
    }

    private void visitIterativeStatement(ParseTreeNode node) {
        // CST Structure for while loop:
        // Child 0: 'while' or 'for' token
        // Child 1: '(' token
        // Child 2: Expression (The condition)
        // Child 3: ')' token
        // Child 4: Statement/CompoundStatement (The loop body)

        Token loopToken = node.getChildren().get(0).getToken();

        if (loopToken.getType() == TokenType.KEYWORD_WHILE) {
            ParseTreeNode conditionNode = node.getChildren().get(2);
            TokenType conditionType = getExpressionType(conditionNode);

            if (conditionType == TokenType.UNKNOWN) {
                logError(loopToken, "Invalid or mixed types inside 'while' loop condition.");
            } else if (conditionType == TokenType.KEYWORD_VOID) {
                logError(loopToken, "Loop condition cannot evaluate to void.");
            }

            // Visit the loop body
            visit(node.getChildren().get(4));
        }
        // Note: For loops have a slightly different CST structure due to initialization and updates,
        // which you can traverse similarly based on your parseIterativeStatement() logic.
    }

    private void visitSelectionStatement(ParseTreeNode node) {
        // Similar to while loops, extract the condition and validate it
        Token ifToken = node.getChildren().get(0).getToken();
        ParseTreeNode conditionNode = node.getChildren().get(2); // The Expression

        TokenType conditionType = getExpressionType(conditionNode);
        if (conditionType == TokenType.UNKNOWN || conditionType == TokenType.KEYWORD_VOID) {
            logError(ifToken, "Invalid condition inside 'if' statement.");
        }

        // Visit the 'if' body
        visit(node.getChildren().get(4));

        // Visit the 'else' tail if it exists
        visit(node.getChildren().get(5));
    }

    private void visitReturnStatement(ParseTreeNode node) {
        // CST Structure:
        // Child 0: 'return' keyword
        // Child 1: Expression (Optional)
        // Child 2: ';' token
        Token returnToken = node.getChildren().get(0).getToken();

        if (node.getChildren().size() == 3) {
            ParseTreeNode exprNode = node.getChildren().get(1);
            TokenType returnType = getExpressionType(exprNode);

            if (returnType == TokenType.UNKNOWN) {
                logError(returnToken, "Invalid expression in return statement.");
            }
            // Future feature: Verify returnType matches the current enclosing function's type
        }
    }

    // --- Statement Handlers with Type Checking ---

    private void visitCompoundStatement(ParseTreeNode node) {
        symbolTable.enterScope();
        for (ParseTreeNode child : node.getChildren()) {
            visit(child);
        }
        symbolTable.exitScope();
    }

    private void visitDeclaration(ParseTreeNode node) {
        // CST structure for declaration:
        // Child 0: Type Token (int, float)
        // Child 1: Identifier Token (x)
        // Child 2: '=' Operator Token (if initialized)
        // Child 3: Expression Node (if initialized)
        Token typeToken = node.getChildren().get(0).getToken();
        Token idToken = node.getChildren().get(1).getToken();

        String varName = idToken.getLexeme();
        TokenType declaredType = typeToken.getType();

        // 1. Scope Rule: Register the variable
        boolean success = symbolTable.declare(varName, declaredType);
        if (!success) {
            logError(idToken, "Variable '" + varName + "' is already declared in this scope.");
            return;
        }

        // 2. Type Rule: If an initialization exists, check type compatibility
        if (node.getChildren().size() > 3) {
            ParseTreeNode exprNode = node.getChildren().get(3);
            TokenType assignedExprType = getExpressionType(exprNode);

            if (assignedExprType == TokenType.UNKNOWN) {
                logError(idToken, "Type mismatch error inside the initialization expression for '" + varName + "'.");
            } else if (assignedExprType != null && assignedExprType != declaredType) {
                logError(idToken, "Type mismatch: Cannot assign " + cleanTypeName(assignedExprType) +
                        " to a variable of type " + cleanTypeName(declaredType) + ".");
            }
        }
    }

    private void visitIdStatement(ParseTreeNode node) {
        // CST structure for assignments:
        // Child 0: Identifier Token
        // Child 1: IdStatementTail -> [Child 0: '=', Child 1: Expression]
        Token idToken = node.getChildren().get(0).getToken();
        String varName = idToken.getLexeme();

        // 1. Scope Rule: Verify declaration exists
        Symbol symbol = symbolTable.lookup(varName);
        if (symbol == null) {
            logError(idToken, "Undeclared variable '" + varName + "'.");
            return;
        }

        // 2. Type Rule: Evaluate right-hand side expression type safety
        ParseTreeNode tailNode = node.getChildren().get(1);
        if (tailNode.getNodeName().equals("IdStatementTail") && !tailNode.getChildren().isEmpty()) {
            ParseTreeNode assignOp = tailNode.getChildren().get(0);

            if (assignOp.getToken() != null && assignOp.getToken().getType() == TokenType.ASSIGN) {
                ParseTreeNode exprNode = tailNode.getChildren().get(1);
                TokenType rightHandSideType = getExpressionType(exprNode);

                if (rightHandSideType == TokenType.UNKNOWN) {
                    logError(idToken, "Type mismatch error within the expression assigned to '" + varName + "'.");
                } else if (rightHandSideType != null && rightHandSideType != symbol.getType()) {
                    logError(idToken, "Type mismatch: Cannot assign " + cleanTypeName(rightHandSideType) +
                            " to variable '" + varName + "' of type " + cleanTypeName(symbol.getType()) + ".");
                }
            }
        }
    }

    // --- Recursive Synthesized Attribute Type Evaluator ---

    /**
     * Recursively walks down an Expression subtree to synthesize its type attribute.
     * If conflicting or mismatched types are blended together, it returns TokenType.UNKNOWN.
     */
    private TokenType getExpressionType(ParseTreeNode node) {
        if (node == null) return null;

        if (node.getToken() != null) {
            Token token = node.getToken();
            if (token.getType() == TokenType.IDENTIFIER) {
                Symbol s = symbolTable.lookup(token.getLexeme());
                return (s != null) ? s.getType() : null;
            }
            if (token.getType() == TokenType.LITERAL_NUM) {
                return token.getLexeme().contains(".") ? TokenType.KEYWORD_FLOAT : TokenType.KEYWORD_INT;
            }
            if (token.getType() == TokenType.LITERAL_CHAR) return TokenType.KEYWORD_CHAR;
            return null;
        }

        // Check if this node is specifically comparing two sides (e.g., Expression < Expression)
        if (node.getNodeName().equals("Expression") && node.getChildren().size() == 3) {
            ParseTreeNode middleChild = node.getChildren().get(1);
            if (middleChild.getToken() != null) {
                TokenType op = middleChild.getToken().getType();
                if (op == TokenType.LT || op == TokenType.GT || op == TokenType.LTE ||
                        op == TokenType.GTE || op == TokenType.EQ || op == TokenType.NEQ) {

                    TokenType leftType = getExpressionType(node.getChildren().get(0));
                    TokenType rightType = getExpressionType(node.getChildren().get(2));

                    if (leftType == TokenType.UNKNOWN || rightType == TokenType.UNKNOWN || leftType != rightType) {
                        return TokenType.UNKNOWN; // Type mismatch in comparison
                    }
                    return TokenType.KEYWORD_INT; // C standards: Relational operators yield an int
                }
            }
        }

        // Standard synthesis for all other branches (Term, Factor, etc.)
        TokenType synthesizedType = null;
        for (ParseTreeNode child : node.getChildren()) {
            TokenType childType = getExpressionType(child);
            if (childType != null) {
                if (childType == TokenType.UNKNOWN) return TokenType.UNKNOWN;
                if (synthesizedType == null) {
                    synthesizedType = childType;
                } else if (synthesizedType != childType) {
                    return TokenType.UNKNOWN;
                }
            }
        }
        return synthesizedType;
    }

    // --- Formatting Helpers ---

    private void logError(Token token, String message) {
        String errorMsg = String.format("Semantic Error at Line %d, Col %d: %s",
                token.getLine(), token.getColumn(), message);
        semanticErrors.add(errorMsg);
    }

    private String cleanTypeName(TokenType type) {
        return switch (type) {
            case KEYWORD_INT -> "int";
            case KEYWORD_FLOAT -> "float";
            case KEYWORD_CHAR -> "char";
            default -> type.name();
        };
    }
}