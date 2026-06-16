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
        // Ignore epsilon elements and leaves representing direct layout tokens
        if (node.getToken() != null || node.getNodeName().equals("ε")) {
            return;
        }

        switch (node.getNodeName()) {
            case "FunctionDefinition" -> visitFunctionDefinition(node);
            case "CompoundStatement" -> visitCompoundStatement(node);
            case "Declaration" -> visitDeclaration(node);
            case "AssignmentStatement" -> visitAssignmentStatement(node);
            case "IterativeStatement" -> visitIterativeStatement(node);
            case "SelectionStatement" -> visitSelectionStatement(node);
            case "ReturnStatement" -> visitReturnStatement(node);
            case "FunctionCallStatement" -> visitFunctionCallStatement(node);
            default -> {
                for (ParseTreeNode child : node.getChildren()) {
                    visit(child);
                }
            }
        }
    }

    private void visitFunctionDefinition(ParseTreeNode node) {
        ParseTreeNode typeNode = null;
        ParseTreeNode idNode = null;
        ParseTreeNode paramListNode = null;
        ParseTreeNode bodyNode = null;

        for (ParseTreeNode child : node.getChildren()) {
            String name = child.getNodeName();
            if (child.getToken() != null && child.getToken().getType() == TokenType.IDENTIFIER) {
                idNode = child;
            } else if (name.contains("Type") || name.equalsIgnoreCase("DataType") ||
                    (child.getToken() != null && isDataTypeToken(child.getToken().getType()))) {
                typeNode = child;
            } else if (name.contains("Parameter") || name.contains("Param") || name.contains("List")) {
                paramListNode = child;
            } else if (name.equals("CompoundStatement")) {
                bodyNode = child;
            }
        }

        if (idNode == null && node.getChildren().size() >= 2) {
            typeNode = node.getChildren().get(0);
            idNode = node.getChildren().get(1);
            if (node.getChildren().size() >= 4) {
                paramListNode = node.getChildren().get(2);
                bodyNode = node.getChildren().get(3);
            }
        }

        if (idNode == null || idNode.getToken() == null) return;

        Token funcToken = idNode.getToken();
        String funcName = funcToken.getLexeme();

        String returnType = "void";
        if (typeNode != null) {
            returnType = extractTypeName(typeNode);
        }

        // Declare the Function symbol globally
        if (!symbolTable.declare(funcName, returnType, "Function", "Global", funcToken.getLine())) {
            logError(funcToken, "Redeclaration Error: Function '" + funcName + "' is already defined.");
        }

        symbolTable.enterScope();
        registerParameters(paramListNode);

        if (bodyNode != null) {
            for (ParseTreeNode child : bodyNode.getChildren()) {
                visit(child);
            }
        }

        symbolTable.exitScope();
    }

    private void visitFunctionCallStatement(ParseTreeNode node) {
        // Sweep the entire function call (including its ArgumentList) for undeclared variables
        checkExpressionIdentifiers(node);
    }

    private void registerParameters(ParseTreeNode node) {
        if (node == null) return;

        if (node.getNodeName().equals("ParameterList")) {
            ParseTreeNode currentTypeNode = null;
            for (ParseTreeNode child : node.getChildren()) {
                if (child.getToken() != null) {
                    // 1. Identify the data type
                    if (isDataTypeToken(child.getToken().getType())) {
                        currentTypeNode = child;
                    }
                    // 2. Identify the parameter name and register the pair
                    else if (child.getToken().getType() == TokenType.IDENTIFIER) {
                        Token pToken = child.getToken();
                        String pType = "int"; // fallback
                        if (currentTypeNode != null) {
                            pType = cleanTypeName(currentTypeNode.getToken().getType());
                        }

                        if (!symbolTable.declare(pToken.getLexeme(), pType, "Parameter", "Local", pToken.getLine())) {
                            logError(pToken, "Redeclaration Error: Duplicate parameter '" + pToken.getLexeme() + "'.");
                        }
                        currentTypeNode = null; // Reset for the next parameter in the list
                    }
                }
            }
        } else {
            // Continue recursion to find the ParameterList
            for (ParseTreeNode child : node.getChildren()) {
                registerParameters(child);
            }
        }
    }

    private void visitReturnStatement(ParseTreeNode node) {
        for (ParseTreeNode child : node.getChildren()) {
            // Sweep the return expression for undeclared identifiers
            if (child.getNodeName().contains("Expression")) {
                checkExpressionIdentifiers(child);
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
        ParseTreeNode typeNode = null;
        boolean isConstant = checkForConstQualifier(node);

        // 1. Locate the base data type node
        for (ParseTreeNode child : node.getChildren()) {
            if (child.getNodeName().contains("Type") || child.getNodeName().equalsIgnoreCase("DataType") ||
                    (child.getToken() != null && isDataTypeToken(child.getToken().getType()))) {
                typeNode = child;
                break;
            }
        }

        String varType = "int";
        if (typeNode != null) {
            varType = extractTypeName(typeNode);
        }

        String detailedType = isConstant ? "const " + varType : varType;
        String category = isConstant ? "Constant" : "Variable";
        String scopeStr = (symbolTable.getCurrentScopeLevel() == 1) ? "Global" : "Local";

        // 2. Sequentially process the flat child list to capture ALL variables
        for (int i = 0; i < node.getChildren().size(); i++) {
            ParseTreeNode child = node.getChildren().get(i);

            // When an identifier is found, it's a new variable in the comma-separated list
            if (child.getToken() != null && child.getToken().getType() == TokenType.IDENTIFIER) {
                Token currentId = child.getToken();
                ParseTreeNode currentExpr = null;

                // Look ahead 2 steps to see if THIS specific identifier has an assignment (= Expression)
                if (i + 2 < node.getChildren().size()) {
                    ParseTreeNode nextNode = node.getChildren().get(i + 1);
                    ParseTreeNode nextNextNode = node.getChildren().get(i + 2);

                    if (nextNode.getToken() != null && nextNode.getToken().getType() == TokenType.ASSIGN) {
                        if (nextNextNode.getNodeName().contains("Expression") || nextNextNode.getNodeName().contains("Assignment")) {
                            currentExpr = nextNextNode;
                            i += 2; // Fast-forward the loop index past the '=' and Expression nodes so we don't re-process them
                        }
                    }
                }
                // Register the paired identifier and expression
                registerSingleVariable(currentId, currentExpr, detailedType, category, scopeStr, varType, isConstant);
            }
        }
    }

    /**
     * Modular method to register a single extracted variable and run its type verification checks.
     */
    private void registerSingleVariable(Token idToken, ParseTreeNode exprNode, String detailedType,
                                        String category, String scopeStr, String varType, boolean isConstant) {
        String varName = idToken.getLexeme();

        // 1. Enforce that constants must have an explicit assignment initialization expression
        if (isConstant && exprNode == null) {
            logError(idToken, "Constant Initialization Error: 'const " + varName + "' must be initialized upon declaration.");
        }

        // 2. Register into the Scope Environment Symbol Table
        if (!symbolTable.declare(varName, detailedType, category, scopeStr, idToken.getLine())) {
            logError(idToken, "Redeclaration Error: " + category + " '" + varName + "' is already defined in this scope.");
            return;
        }

        // 3. Evaluate right-hand side subexpression if present
        if (exprNode != null) {
            // First pass check: capture any unknown variables used inside the initializer expression itself
            checkExpressionIdentifiers(exprNode);

            TokenType targetEnum = convertStringToEnum(varType);
            TokenType evaluatedEnum = getExpressionType(exprNode);

            if (evaluatedEnum != TokenType.UNKNOWN && !isCompatible(targetEnum, evaluatedEnum)) {
                logError(idToken, "Type Mismatch Error: Cannot initialize variable of type '" +
                        detailedType + "' with an expression of type '" + cleanTypeName(evaluatedEnum) + "'.");
            }
        }
    }

    private void visitAssignmentStatement(ParseTreeNode node) {
        ParseTreeNode idNode = null;
        ParseTreeNode exprNode = null;

        // 1. Locate the identifier and expression children using your original logic
        for (ParseTreeNode child : node.getChildren()) {
            if (child.getToken() != null && child.getToken().getType() == TokenType.IDENTIFIER) {
                // Ensure we only grab the left-hand identifier, not identifiers inside the expression
                if (idNode == null) idNode = child;
            } else if (child.getNodeName().contains("Expression") || child.getNodeName().contains("Assignment")) {
                exprNode = child;
            }
        }

        if (idNode == null && !node.getChildren().isEmpty()) {
            idNode = node.getChildren().get(0);
            if (node.getChildren().size() >= 3) {
                exprNode = node.getChildren().get(2);
            }
        }

        if (idNode == null || idNode.getToken() == null) return;

        // 2. Validate the left-hand side target identifier variable
        Token idToken = idNode.getToken();
        Symbol symbol = symbolTable.lookup(idToken.getLexeme());

        if (symbol == null) {
            logError(idToken, "Undeclared Identifier Error: Variable '" + idToken.getLexeme() + "' has not been declared.");
            return;
        }

        // Immutability Check: Intercept mutations on read-only constant spaces
        if (symbol.getType().startsWith("const ")) {
            logError(idToken, "Assignment Error: Cannot modify read-only constant variable '" + idToken.getLexeme() + "'.");
            return;
        }

        // 3. Process the right-hand side expression structure safely
        if (exprNode != null) {
            // Run deep recursive check to catch undeclared variables inside the expression tree
            checkExpressionIdentifiers(exprNode);

            TokenType targetEnum = convertStringToEnum(symbol.getType());
            TokenType evaluatedEnum = getExpressionType(exprNode);

            if (evaluatedEnum != TokenType.UNKNOWN && !isCompatible(targetEnum, evaluatedEnum)) {
                logError(idToken, "Type Mismatch Error: Cannot assign value of type '" +
                        cleanTypeName(evaluatedEnum) + "' to variable '" + idToken.getLexeme() + "' of type '" + symbol.getType() + "'.");
            }
        }
    }

    private void visitIterativeStatement(ParseTreeNode node) {
        // 1. Scan configuration/condition children for undeclared identifiers
        for (ParseTreeNode child : node.getChildren()) {
            String name = child.getNodeName();
            if (!name.equals("CompoundStatement") && !name.endsWith("Statement") && child.getToken() == null) {
                checkExpressionIdentifiers(child);
            }
        }

        // 2. Process the inner block loop contents normally
        for (ParseTreeNode child : node.getChildren()) {
            visit(child);
        }
    }

    private void visitSelectionStatement(ParseTreeNode node) {
        // 1. Scan structural condition children (skipping the executable body block)
        for (ParseTreeNode child : node.getChildren()) {
            String name = child.getNodeName();
            if (!name.equals("CompoundStatement") && !name.endsWith("Statement") && child.getToken() == null) {
                checkExpressionIdentifiers(child);
            }
        }

        // 2. Evaluate the inner block scopes normally
        for (ParseTreeNode child : node.getChildren()) {
            visit(child);
        }
    }

    private TokenType getExpressionType(ParseTreeNode node) {
        if (node == null) return null;

        if (node.getToken() != null) {
            Token t = node.getToken();
            if (t.getType() == TokenType.LITERAL_NUM) {
                // Numeric type categorization handling double constants vs fallback floats/ints
                if (t.getLexeme().contains(".")) {
                    return t.getLexeme().toLowerCase().endsWith("d") ? TokenType.KEYWORD_DOUBLE : TokenType.KEYWORD_FLOAT;
                }
                return TokenType.KEYWORD_INT;
            }
            if (t.getType() == TokenType.LITERAL_CHAR) return TokenType.KEYWORD_CHAR;
            if (t.getType() == TokenType.IDENTIFIER) {
                Symbol s = symbolTable.lookup(t.getLexeme());
                if (s == null) {
                    logError(t, "Undeclared Identifier Error: '" + t.getLexeme() + "' used in expression evaluation.");
                    return TokenType.UNKNOWN;
                }
                return convertStringToEnum(s.getType());
            }
            return null;
        }

        TokenType structuralType = null;
        for (ParseTreeNode child : node.getChildren()) {
            TokenType childType = getExpressionType(child);
            if (childType != null) {
                if (childType == TokenType.UNKNOWN) return TokenType.UNKNOWN;
                if (structuralType == null) {
                    structuralType = childType;
                } else if (structuralType != childType) {
                    // Type promotion tier ordering logic: int -> float -> double
                    structuralType = determinePromotedType(structuralType, childType);
                }
            }
        }
        return structuralType;
    }

    private TokenType determinePromotedType(TokenType t1, TokenType t2) {
        if (t1 == TokenType.KEYWORD_DOUBLE || t2 == TokenType.KEYWORD_DOUBLE) return TokenType.KEYWORD_DOUBLE;
        if (t1 == TokenType.KEYWORD_FLOAT || t2 == TokenType.KEYWORD_FLOAT) return TokenType.KEYWORD_FLOAT;
        if (t1 == TokenType.KEYWORD_INT || t2 == TokenType.KEYWORD_INT) return TokenType.KEYWORD_INT;
        return TokenType.UNKNOWN;
    }

    private boolean isCompatible(TokenType target, TokenType evaluated) {
        if (target == evaluated) return true;
        // Widening type coercion rules
        if (target == TokenType.KEYWORD_DOUBLE) {
            return (evaluated == TokenType.KEYWORD_FLOAT || evaluated == TokenType.KEYWORD_INT);
        }
        if (target == TokenType.KEYWORD_FLOAT) {
            return (evaluated == TokenType.KEYWORD_INT);
        }
        return false;
    }

    private boolean checkForConstQualifier(ParseTreeNode node) {
        if (node == null) return false;
        if (node.getToken() != null && node.getToken().getType() == TokenType.KEYWORD_CONST) {
            return true;
        }
        for (ParseTreeNode child : node.getChildren()) {
            // Check structural syntax container heads for immediate child leaves pointing to const
            if (child.getToken() != null && child.getToken().getType() == TokenType.KEYWORD_CONST) {
                return true;
            }
        }
        return false;
    }

    private String extractTypeName(ParseTreeNode node) {
        String type = findDataTypeInTree(node);
        return type != null ? type : "int"; // Fallback to int if nothing is found
    }

    private String findDataTypeInTree(ParseTreeNode node) {
        if (node == null) return null;

        // If this node contains a valid data type token, return its cleaned name
        if (node.getToken() != null && isDataTypeToken(node.getToken().getType())) {
            return cleanTypeName(node.getToken().getType());
        }

        // Otherwise, recursively search deeper into the children
        for (ParseTreeNode child : node.getChildren()) {
            String found = findDataTypeInTree(child);
            if (found != null) return found;
        }
        return null;
    }

    private boolean isDataTypeToken(TokenType type) {
        return type == TokenType.KEYWORD_INT || type == TokenType.KEYWORD_FLOAT ||
                type == TokenType.KEYWORD_DOUBLE || type == TokenType.KEYWORD_CHAR ||
                type == TokenType.KEYWORD_VOID;
    }

    private void logError(Token token, String message) {
        errorLog.add(new SemanticException(message, token.getLine(), token.getColumn()));
    }

    private String cleanTypeName(TokenType type) {
        return switch (type) {
            case KEYWORD_INT -> "int";
            case KEYWORD_FLOAT -> "float";
            case KEYWORD_DOUBLE -> "double";
            case KEYWORD_CHAR -> "char";
            case KEYWORD_VOID -> "void";
            default -> type.name();
        };
    }

    private TokenType convertStringToEnum(String typeStr) {
        if (typeStr == null) return TokenType.UNKNOWN;
        String workingStr = typeStr.replace("const ", "").trim();
        return switch (workingStr) {
            case "int" -> TokenType.KEYWORD_INT;
            case "float" -> TokenType.KEYWORD_FLOAT;
            case "double" -> TokenType.KEYWORD_DOUBLE;
            case "char" -> TokenType.KEYWORD_CHAR;
            case "void" -> TokenType.KEYWORD_VOID;
            default -> TokenType.UNKNOWN;
        };
    }

    private void checkExpressionIdentifiers(ParseTreeNode node) {
        if (node == null) return;

        // If this node is a leaf holding an identifier token, validate it!
        if (node.getToken() != null && node.getToken().getType() == TokenType.IDENTIFIER) {
            String varName = node.getToken().getLexeme();
            if (symbolTable.lookup(varName) == null) {
                logError(node.getToken(), "Undeclared identifier '" + varName + "'");
            }
        }

        // Recursively sweep through the entire expression subtree (e.g. operators, literals)
        for (ParseTreeNode child : node.getChildren()) {
            checkExpressionIdentifiers(child);
        }
    }
}