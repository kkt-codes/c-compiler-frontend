package com.compiler.parser;

import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errorLog = new ArrayList<>();

    // Internal exception for Panic Mode routing
    private static class ParseException extends RuntimeException {}

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // --- Entry Point ---

    public ParseTreeNode parse() {
        ParseTreeNode root = new ParseTreeNode("Program'");

        while (!isAtEnd()) {
            try {
                root.addChild(parseStatement());
            } catch (ParseException error) {
                int lastIndex = current;
                synchronize();

                // Safety Check: Only force an advance if synchronize() got completely stuck
                // and did NOT stop on a valid statement-starting keyword.
                if (current == lastIndex && !isAtEnd()) {
                    switch (peek().getType()) {
                        case KEYWORD_INT:
                        case KEYWORD_FLOAT:
                        case KEYWORD_CHAR:
                        case KEYWORD_VOID:
                        case KEYWORD_IF:
                        case KEYWORD_WHILE:
                        case KEYWORD_FOR:
                        case KEYWORD_RETURN:
                            // Safe to leave it alone; parseStatement() will handle it.
                            break;
                        default:
                            // Truly stuck on bad syntax lookahead. Skip it to prevent memory leak.
                            advance();
                            break;
                    }
                }
            }
        }
        return root;
    }

    public List<String> getErrorLog() {
        return errorLog;
    }

    // --- Core Recursive Descent Methods (Skeleton) ---

    private ParseTreeNode parseExpression() {
        ParseTreeNode node = new ParseTreeNode("Expression");
        node.addChild(parseAdditiveExpression());

        if (match(TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE, TokenType.EQ, TokenType.NEQ)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseAdditiveExpression());
        }

        return node;
    }

    private ParseTreeNode parseAdditiveExpression() {
        ParseTreeNode node = new ParseTreeNode("Expression");
        node.addChild(parseTerm());
        node.addChild(parseAdditiveExpressionPrime());
        return node;
    }

    private ParseTreeNode parseAdditiveExpressionPrime() {
        ParseTreeNode node = new ParseTreeNode("ExpressionPrime");

        // If we see a + or -, consume it and chain the next term
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            node.addChild(new ParseTreeNode(previous())); // The operator
            node.addChild(parseTerm());                   // The right side of the math
            node.addChild(parseAdditiveExpressionPrime());        // Recursive call for chaining (e.g., 5 + 3 + 2)
        } else {
            // Epsilon fallback: No more addition/subtraction to do
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseTerm() {
        ParseTreeNode node = new ParseTreeNode("Term");
        node.addChild(parseFactor());
        node.addChild(parseTermPrime());
        return node;
    }

    private ParseTreeNode parseTermPrime() {
        ParseTreeNode node = new ParseTreeNode("TermPrime");

        if (match(TokenType.MULT, TokenType.DIV)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseFactor());
            node.addChild(parseTermPrime());
        } else {
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseFactor() {
        ParseTreeNode node = new ParseTreeNode("Factor");

        if (match(TokenType.LITERAL_NUM, TokenType.LITERAL_CHAR)) {
            // It's a raw number or character
            node.addChild(new ParseTreeNode(previous()));

        } else if (match(TokenType.IDENTIFIER)) {
            // It's a variable or a function call!
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseIdFactorTail()); // Resolve the left-factoring

        } else if (match(TokenType.LPAREN)) {
            // It's a nested math expression: ( ... )
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseExpression());
            Token rParen = consume(TokenType.RPAREN, "Expected ')' after expression.");
            node.addChild(new ParseTreeNode(rParen));

        } else {
            throw error(peek(), "Expected a number, identifier, or '(' in expression.");
        }

        return node;
    }

    private ParseTreeNode parseIdFactorTail() {
        ParseTreeNode node = new ParseTreeNode("IdFactorTail");

        // If the identifier is immediately followed by '(', it's a function call!
        if (match(TokenType.LPAREN)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseArgumentList());
            Token rParen = consume(TokenType.RPAREN, "Expected ')' after function arguments.");
            node.addChild(new ParseTreeNode(rParen));
        } else {
            // Epsilon fallback: It's just a standard variable
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseArgumentList() {
        ParseTreeNode node = new ParseTreeNode("ArgumentList");

        // If the next token is NOT a closing parenthesis, we have arguments to parse
        if (!check(TokenType.RPAREN)) {
            node.addChild(parseExpression());
            node.addChild(parseArgumentTail());
        } else {
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseArgumentTail() {
        ParseTreeNode node = new ParseTreeNode("ArgumentTail");

        // Check for subsequent comma-separated arguments
        if (match(TokenType.COMMA)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseExpression());
            node.addChild(parseArgumentTail());
        } else {
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseStatement() {
        // Look at the FIRST set to determine which rule to apply
        if (match(TokenType.KEYWORD_IF)) {
            return parseSelectionStatement();
        }
        if (match(TokenType.KEYWORD_WHILE, TokenType.KEYWORD_FOR)) {
            return parseIterativeStatement();
        }
        if (match(TokenType.KEYWORD_RETURN)) {
            return parseReturnStatement();
        }
        if (match(TokenType.LBRACE)) {
            return parseCompoundStatement();
        }
        if (match(TokenType.KEYWORD_INT, TokenType.KEYWORD_FLOAT,
                TokenType.KEYWORD_CHAR, TokenType.KEYWORD_VOID)) {
            // Un-consume the type token so the Declaration method can use it
            current--;
            return parseDeclaration();
        }
        if (match(TokenType.IDENTIFIER)) {
            // Un-consume to let the IdStatement (left-factored rule) handle it
            //current--;
            // led to infinite loop, solution below
            return parseIdStatement(previous()); // Pass the identifier token
        }

        // If it doesn't match any FIRST set of a statement, it's an error!
        throw error(peek(), "Expected a statement.");
    }

    // Placeholders for the specific grammar rules we defined...
    private ParseTreeNode parseSelectionStatement() {
        ParseTreeNode node = new ParseTreeNode("SelectionStatement");

        // 1. Add the 'if' keyword token (already consumed by parseStatement)
        node.addChild(new ParseTreeNode(previous()));

        // 2. Consume '('
        Token lParen = consume(TokenType.LPAREN, "Expected '(' after 'if'.");
        node.addChild(new ParseTreeNode(lParen));

        // 3. Parse the condition expression
        node.addChild(parseExpression());

        // 4. Consume ')'
        Token rParen = consume(TokenType.RPAREN, "Expected ')' after if condition.");
        node.addChild(new ParseTreeNode(rParen));

        // 5. Parse the body of the 'if' statement
        node.addChild(parseStatement());

        // 6. SelectionTail: Check for an optional 'else' block
        ParseTreeNode tailNode = new ParseTreeNode("SelectionTail");
        if (match(TokenType.KEYWORD_ELSE)) {
            tailNode.addChild(new ParseTreeNode(previous())); // The 'else' token
            tailNode.addChild(parseStatement());              // The body of the 'else'
        } else {
            // Epsilon fallback: No else clause present
            tailNode.addChild(new ParseTreeNode("ε", true));
        }
        node.addChild(tailNode);

        return node;
    }

    private ParseTreeNode parseIterativeStatement() {
        ParseTreeNode node = new ParseTreeNode("IterativeStatement");
        Token loopKeyword = previous(); // Captures either 'while' or 'for'
        node.addChild(new ParseTreeNode(loopKeyword));

        // Both loops require a starting '('
        Token lParen = consume(TokenType.LPAREN, "Expected '(' after loop keyword.");
        node.addChild(new ParseTreeNode(lParen));

        if (loopKeyword.getType() == TokenType.KEYWORD_WHILE) {
            // --- While Loop Setup ---
            node.addChild(parseExpression());

            Token rParen = consume(TokenType.RPAREN, "Expected ')' after while condition.");
            node.addChild(new ParseTreeNode(rParen));

            // Loop body
            node.addChild(parseStatement());

        } else {
            // --- For Loop Setup ---
            // Clause 1: Initialization (e.g., int i = 0 or i = 0)
            if (match(TokenType.KEYWORD_INT, TokenType.KEYWORD_FLOAT, TokenType.KEYWORD_CHAR)) {
                current--; // Step back to allow parseDeclaration to handle the type token
                node.addChild(parseDeclaration()); // Handles declaration + trailing semicolon
            } else if (!check(TokenType.SEMICOLON)) {
                node.addChild(parseExpression());
                Token semi = consume(TokenType.SEMICOLON, "Expected ';' after for loop initialization.");
                node.addChild(new ParseTreeNode(semi));
            } else {
                Token semi = consume(TokenType.SEMICOLON, "Expected ';' after for loop initialization.");
                node.addChild(new ParseTreeNode(semi));
            }

            // Clause 2: Loop Condition
            if (!check(TokenType.SEMICOLON)) {
                node.addChild(parseExpression());
            }
            Token semi2 = consume(TokenType.SEMICOLON, "Expected ';' after for loop condition.");
            node.addChild(new ParseTreeNode(semi2));

            // Clause 3: Loop Increment/Update
            if (!check(TokenType.RPAREN)) {
                node.addChild(parseExpression());
            }
            Token rParen = consume(TokenType.RPAREN, "Expected ')' after for loop headers.");
            node.addChild(new ParseTreeNode(rParen));

            // Loop body
            node.addChild(parseStatement());
        }

        return node;
    }

    private ParseTreeNode parseReturnStatement() {
        ParseTreeNode node = new ParseTreeNode("ReturnStatement");

        // The 'return' keyword was already matched in parseStatement()
        node.addChild(new ParseTreeNode(previous()));

        // ReturnTail: If the next token isn't a semicolon, it must be an expression
        if (!check(TokenType.SEMICOLON)) {
            node.addChild(parseExpression());
        }

        Token semiToken = consume(TokenType.SEMICOLON, "Expected ';' after return statement.");
        node.addChild(new ParseTreeNode(semiToken));

        return node;
    }

    private ParseTreeNode parseCompoundStatement() {
        ParseTreeNode node = new ParseTreeNode("CompoundStatement");
        node.addChild(new ParseTreeNode(previous())); // Adds the '{' token
        ParseTreeNode statementList = new ParseTreeNode("StatementList");

        // Keep parsing statements until we hit '}' or the file ends unexpectedly
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statementList.addChild(parseStatement());
        }
        node.addChild(statementList);

        // Consume the closing brace
        Token rbrace = consume(TokenType.RBRACE, "Expected '}' to close block.");
        node.addChild(new ParseTreeNode(rbrace));

        return node;
    }

    private ParseTreeNode parseDeclaration() {
        ParseTreeNode node = new ParseTreeNode("Declaration");

        // 1. Consume Type (int, float, char, void)
        Token typeToken = consumeType("Expected a variable type.");
        node.addChild(new ParseTreeNode(typeToken));

        // 2. Consume Identifier (the variable name)
        Token idToken = consume(TokenType.IDENTIFIER, "Expected variable name after type.");
        node.addChild(new ParseTreeNode(idToken));

        // 3. DeclarationTail (Handling the optional assignment)
        if (match(TokenType.ASSIGN)) {
            node.addChild(new ParseTreeNode(previous())); // The '=' token

            node.addChild(parseExpression());
        }

        // 4. Expect Semicolon
        Token semiToken = consume(TokenType.SEMICOLON, "Expected ';' after declaration.");
        node.addChild(new ParseTreeNode(semiToken));

        return node;
    }

    // Helper method just for the data types
    private Token consumeType(String errorMessage) {
        if (match(TokenType.KEYWORD_INT, TokenType.KEYWORD_FLOAT,
                TokenType.KEYWORD_CHAR, TokenType.KEYWORD_VOID)) {
            return previous();
        }
        throw error(peek(), errorMessage);
    }

    private ParseTreeNode parseIdStatement(Token idToken) {
        ParseTreeNode node = new ParseTreeNode("IdStatement");

        // 1. Consume the identifier (we already peeked and verified it)
        //Token idToken = consume(TokenType.IDENTIFIER, "Expected identifier.");
        node.addChild(new ParseTreeNode(idToken));

        // 2. Delegate to the tail rule to decide if it's an assignment or a function call
        node.addChild(parseIdStatementTail());

        // 3. Every complete statement must end with a semicolon
        Token semiToken = consume(TokenType.SEMICOLON, "Expected ';' after statement.");
        node.addChild(new ParseTreeNode(semiToken));

        return node;
    }

    private ParseTreeNode parseIdStatementTail() {
        ParseTreeNode node = new ParseTreeNode("IdStatementTail");

        if (match(TokenType.ASSIGN)) {
            // Option A: It's an assignment statement (e.g., x = 5)
            node.addChild(new ParseTreeNode(previous())); // Adds the '=' token
            node.addChild(parseExpression());
        } else if (match(TokenType.LPAREN)) {
            // Option B: It's a standalone function call statement (e.g., functionName())
            node.addChild(new ParseTreeNode(previous())); // Adds the '(' token
            node.addChild(parseArgumentList());
            Token rParen = consume(TokenType.RPAREN, "Expected ')' after function arguments.");
            node.addChild(new ParseTreeNode(rParen));
        } else {
            throw error(peek(), "Expected '=' or '(' after identifier in statement.");
        }

        return node;
    }


    // --- Panic Mode Recovery ---

    private void synchronize() {
        while (!isAtEnd()) {
            // If the previous token was a semicolon, the current token is a fresh start.
            if (previous().getType() == TokenType.SEMICOLON) {
                return;
            }

            // If the CURRENT token legally starts a statement, stop immediately!
            // Do NOT consume it; let parseStatement() handle it on the next loop.
            switch (peek().getType()) {
                case KEYWORD_INT:
                case KEYWORD_FLOAT:
                case KEYWORD_CHAR:
                case KEYWORD_VOID:
                case KEYWORD_IF:
                case KEYWORD_WHILE:
                case KEYWORD_FOR:
                case KEYWORD_RETURN:
                    return;
            }

            // It's a genuine garbage token within a broken statement. Skip it.
            advance();
        }
    }

    private ParseException error(Token token, String message) {
        String errorMsg = "Syntax Error at Line " + token.getLine() +
                ", Col " + token.getColumn() +
                " ['" + token.getLexeme() + "']: " + message;
        errorLog.add(errorMsg);
        return new ParseException();
    }

    // --- Parser Helpers (Lookahead & Consumption) ---

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}