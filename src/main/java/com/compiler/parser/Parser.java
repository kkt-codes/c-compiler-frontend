package com.compiler.parser;

import com.compiler.exception.SyntaxException;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<SyntaxException> errorLog = new ArrayList<>();

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
                // If it starts with a data type, it's either a declaration or a function definition
                if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_FLOAT) ||
                        check(TokenType.KEYWORD_CHAR) || check(TokenType.KEYWORD_VOID)) {
                    root.addChild(parseDeclarationOrFunction());
                } else {
                    root.addChild(parseStatement());
                }
            } catch (ParseException error) {
                int lastIndex = current;
                synchronize();
                if (current == lastIndex && !isAtEnd()) advance();
            }
        }
        return root;
    }

    public List<SyntaxException> getErrorLog() {
        return errorLog;
    }

    // --- Core Recursive Descent Grammar Implementation ---

    private ParseTreeNode parseDeclarationOrFunction() {
        Token typeToken = advance(); // Consume fundamental C data type keyword
        Token id = consume(TokenType.IDENTIFIER, "Expected identifier name.");

        if (check(TokenType.LPAREN)) {
            // Found a function definition signature: type id ( parameters ) { ... }
            ParseTreeNode node = new ParseTreeNode("FunctionDefinition");
            node.addChild(new ParseTreeNode(typeToken));
            node.addChild(new ParseTreeNode(id));
            node.addChild(new ParseTreeNode(advance())); // Consume '('

            node.addChild(parseParameterList());

            consume(TokenType.RPAREN, "Expected ')' after parameter signatures.");
            node.addChild(new ParseTreeNode(previous()));

            node.addChild(parseCompoundStatement());
            return node;
        } else {
            // Found a standard declaration statement
            ParseTreeNode node = new ParseTreeNode("Declaration");
            node.addChild(new ParseTreeNode(typeToken));
            node.addChild(new ParseTreeNode(id));

            if (match(TokenType.ASSIGN)) {
                node.addChild(new ParseTreeNode(previous())); // '='
                node.addChild(parseExpression());
            }
            consume(TokenType.SEMICOLON, "Expected ';' after variable declaration statement.");
            node.addChild(new ParseTreeNode(previous()));
            return node;
        }
    }

    private ParseTreeNode parseParameterList() {
        ParseTreeNode node = new ParseTreeNode("ParameterList");
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_FLOAT) ||
                check(TokenType.KEYWORD_CHAR) || check(TokenType.KEYWORD_VOID)) {

            node.addChild(new ParseTreeNode(advance())); // Type keyword
            node.addChild(new ParseTreeNode(consume(TokenType.IDENTIFIER, "Expected parameter identification name.")));

            while (match(TokenType.COMMA)) {
                node.addChild(new ParseTreeNode(previous())); // ','
                if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_FLOAT) ||
                        check(TokenType.KEYWORD_CHAR) || check(TokenType.KEYWORD_VOID)) {
                    node.addChild(new ParseTreeNode(advance()));
                    node.addChild(new ParseTreeNode(consume(TokenType.IDENTIFIER, "Expected parameter identification name.")));
                } else {
                    throw error(peek(), "Expected formal type descriptor following comma delimiter.");
                }
            }
        } else {
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    private ParseTreeNode parseStatement() {
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_FLOAT) ||
                check(TokenType.KEYWORD_CHAR) || check(TokenType.KEYWORD_VOID)) {
            return parseDeclarationOrFunction();
        }
        if (match(TokenType.KEYWORD_IF)) return parseSelectionStatement();
        if (match(TokenType.KEYWORD_WHILE)) return parseIterativeStatement();
        if (match(TokenType.KEYWORD_FOR)) return parseForStatement();
        if (match(TokenType.KEYWORD_RETURN)) return parseReturnStatement();
        if (check(TokenType.LBRACE)) return parseCompoundStatement();

        // Lookahead check to parse explicit Function Call Statements separately from Assignment
        if (check(TokenType.IDENTIFIER) && peekNext().getType() == TokenType.LPAREN) {
            return parseFunctionCallStatement();
        }

        return parseAssignmentStatement();
    }

    private ParseTreeNode parseSelectionStatement() {
        ParseTreeNode node = new ParseTreeNode("SelectionStatement");
        node.addChild(new ParseTreeNode(previous())); // 'if'
        node.addChild(new ParseTreeNode(consume(TokenType.LPAREN, "Expected '(' after 'if' statement condition keyword.")));
        node.addChild(parseExpression());
        node.addChild(new ParseTreeNode(consume(TokenType.RPAREN, "Expected ')' concluding statement condition configuration.")));
        node.addChild(parseStatement());

        if (match(TokenType.KEYWORD_ELSE)) {
            node.addChild(new ParseTreeNode(previous())); // 'else'
            node.addChild(parseStatement());
        }
        return node;
    }

    private ParseTreeNode parseIterativeStatement() {
        ParseTreeNode node = new ParseTreeNode("IterativeStatement");
        node.addChild(new ParseTreeNode(previous())); // 'while'
        node.addChild(new ParseTreeNode(consume(TokenType.LPAREN, "Expected '(' after 'while' statement iteration keyword.")));
        node.addChild(parseExpression());
        node.addChild(new ParseTreeNode(consume(TokenType.RPAREN, "Expected ')' concluding iteration condition loop specification.")));
        node.addChild(parseStatement());
        return node;
    }

    private ParseTreeNode parseForStatement() {
        ParseTreeNode node = new ParseTreeNode("ForStatement");
        node.addChild(new ParseTreeNode(previous())); // 'for'
        node.addChild(new ParseTreeNode(consume(TokenType.LPAREN, "Expected '(' after 'for' statement configuration keyword.")));

        // Part I: Initialization
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_FLOAT) ||
                check(TokenType.KEYWORD_CHAR) || check(TokenType.KEYWORD_VOID)) {
            node.addChild(parseDeclarationOrFunction());
        } else if (match(TokenType.SEMICOLON)) {
            node.addChild(new ParseTreeNode(previous()));
        } else {
            node.addChild(parseAssignmentStatement());
        }

        // Part II: Loop Condition
        if (!check(TokenType.SEMICOLON)) {
            node.addChild(parseExpression());
        }
        node.addChild(new ParseTreeNode(consume(TokenType.SEMICOLON, "Expected ';' following for loop internal conditional specification.")));

        // Part III: Increment Expression
        if (!check(TokenType.RPAREN)) {
            node.addChild(parseExpression());
        }
        node.addChild(new ParseTreeNode(consume(TokenType.RPAREN, "Expected ')' matching initialization clauses.")));
        node.addChild(parseStatement());
        return node;
    }

    private ParseTreeNode parseReturnStatement() {
        ParseTreeNode node = new ParseTreeNode("ReturnStatement");
        node.addChild(new ParseTreeNode(previous())); // 'return'
        if (!check(TokenType.SEMICOLON)) {
            node.addChild(parseExpression());
        }
        node.addChild(new ParseTreeNode(consume(TokenType.SEMICOLON, "Expected ';' terminating explicit return statement routing.")));
        return node;
    }

    private ParseTreeNode parseCompoundStatement() {
        ParseTreeNode node = new ParseTreeNode("CompoundStatement");
        node.addChild(new ParseTreeNode(consume(TokenType.LBRACE, "Expected entry '{' scope descriptor bracket.")));
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            node.addChild(parseStatement());
        }
        node.addChild(new ParseTreeNode(consume(TokenType.RBRACE, "Expected exiting '}' scope delimiter bracket.")));
        return node;
    }

    private ParseTreeNode parseAssignmentStatement() {
        ParseTreeNode node = new ParseTreeNode("AssignmentStatement");
        node.addChild(new ParseTreeNode(consume(TokenType.IDENTIFIER, "Expected left-hand identifier target variable location.")));
        node.addChild(new ParseTreeNode(consume(TokenType.ASSIGN, "Expected '=' assignment mapping operator.")));
        node.addChild(parseExpression());
        node.addChild(new ParseTreeNode(consume(TokenType.SEMICOLON, "Expected ';' terminating assignment statement configuration.")));
        return node;
    }

    private ParseTreeNode parseFunctionCallStatement() {
        ParseTreeNode node = new ParseTreeNode("FunctionCallStatement");
        node.addChild(parseFunctionCall());
        node.addChild(new ParseTreeNode(consume(TokenType.SEMICOLON, "Expected ';' terminating functional expression action calls.")));
        return node;
    }

    private ParseTreeNode parseFunctionCall() {
        ParseTreeNode node = new ParseTreeNode("FunctionCall");
        node.addChild(new ParseTreeNode(consume(TokenType.IDENTIFIER, "Expected callable expression function reference name.")));
        node.addChild(new ParseTreeNode(consume(TokenType.LPAREN, "Expected open parenthetical argument configuration boundary.")));
        node.addChild(parseArgumentList());
        node.addChild(new ParseTreeNode(consume(TokenType.RPAREN, "Expected close parenthetical argument configuration boundary.")));
        return node;
    }

    private ParseTreeNode parseArgumentList() {
        ParseTreeNode node = new ParseTreeNode("ArgumentList");
        if (!check(TokenType.RPAREN)) {
            node.addChild(parseExpression());
            while (match(TokenType.COMMA)) {
                node.addChild(new ParseTreeNode(previous())); // ','
                node.addChild(parseExpression());
            }
        } else {
            node.addChild(new ParseTreeNode("ε", true));
        }
        return node;
    }

    // --- Expression Precedence Hierarchy (Arithmetic and Boolean) ---

    public ParseTreeNode parseExpression() {
        ParseTreeNode node = new ParseTreeNode("Expression");
        node.addChild(parseArithmeticExpression());

        // Relational & Boolean Operator Precedence Layer
        if (match(TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE)) {
            node.addChild(new ParseTreeNode(previous())); // Operator terminal
            node.addChild(parseArithmeticExpression());  // Right-hand evaluation chain
        }
        return node;
    }

    private ParseTreeNode parseArithmeticExpression() {
        ParseTreeNode node = new ParseTreeNode("ArithmeticExpression");
        node.addChild(parseTerm());
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseTerm());
        }
        return node;
    }

    private ParseTreeNode parseTerm() {
        ParseTreeNode node = new ParseTreeNode("Term");
        node.addChild(parseFactor());
        while (match(TokenType.MULT, TokenType.DIV)) {
            node.addChild(new ParseTreeNode(previous()));
            node.addChild(parseFactor());
        }
        return node;
    }

    private ParseTreeNode parseFactor() {
        ParseTreeNode node = new ParseTreeNode("Factor");
        if (match(TokenType.LITERAL_NUM, TokenType.LITERAL_CHAR)) {
            node.addChild(new ParseTreeNode(previous()));
        } else if (check(TokenType.IDENTIFIER)) {
            if (peekNext().getType() == TokenType.LPAREN) {
                node.addChild(parseFunctionCall()); // Handles inline function evaluations cleanly
            } else {
                node.addChild(new ParseTreeNode(advance()));
            }
        } else if (match(TokenType.LPAREN)) {
            node.addChild(new ParseTreeNode(previous())); // '('
            node.addChild(parseExpression());
            node.addChild(new ParseTreeNode(consume(TokenType.RPAREN, "Expected matching closing expression parenthesis symbol.")));
        } else {
            throw error(peek(), "Expected matching expression context factor such as variables, constants, or parenthetical groups.");
        }
        return node;
    }

    // --- Helper Frameworks ---

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) return;

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
            advance();
        }
    }

    private ParseException error(Token token, String message) {
        // 1. Create and log the detailed checked exception for UI/logs
        SyntaxException exception = new SyntaxException(message, token.getLine(), token.getColumn());
        errorLog.add(exception);

        // 2. Return the internal RuntimeException to unwind the parser stack safely
        return new ParseException();
    }

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

    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.getLast();
        return tokens.get(current + 1);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}