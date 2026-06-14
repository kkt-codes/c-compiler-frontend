package com.compiler.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // Tracking state
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startColumn = 1;

    // Keyword mapping
    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("int", TokenType.KEYWORD_INT);
        keywords.put("float", TokenType.KEYWORD_FLOAT);
        keywords.put("char", TokenType.KEYWORD_CHAR);
        keywords.put("void", TokenType.KEYWORD_VOID);
        keywords.put("if", TokenType.KEYWORD_IF);
        keywords.put("else", TokenType.KEYWORD_ELSE);
        keywords.put("while", TokenType.KEYWORD_WHILE);
        keywords.put("for", TokenType.KEYWORD_FOR);
        keywords.put("return", TokenType.KEYWORD_RETURN);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // Single-character tokens
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.MULT); break;

            // Operators that might be one or two characters
            case '=': addToken(match('=') ? TokenType.EQ : TokenType.ASSIGN); break;
            case '!': addToken(match('=') ? TokenType.NEQ : TokenType.UNKNOWN); break;
            case '<': addToken(match('=') ? TokenType.LTE : TokenType.LT); break;
            case '>': addToken(match('=') ? TokenType.GTE : TokenType.GT); break;

            // Division or Comments
            case '/':
                if (match('/')) {
                    // Single-line comment: keep reading until newline
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // Multi-line comment: keep reading until */
                    while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
                        if (peek() == '\n') {
                            line++;
                            column = 0; // Reset column on newline
                        }
                        advance();
                    }
                    if (!isAtEnd()) {
                        advance(); // Consume '*'
                        advance(); // Consume '/'
                    }
                } else {
                    addToken(TokenType.DIV);
                }
                break;

            // Whitespace
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                column = 1;
                break;

            // Character Literals
            case '\'':
                character();
                break;

            // Default: Numbers, Identifiers, or Errors
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    // Graceful error handling for unrecognized characters
                    addToken(TokenType.UNKNOWN, Character.toString(c));
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the "."
            while (isDigit(peek())) advance();
            addToken(TokenType.LITERAL_NUM);
        } else {
            addToken(TokenType.LITERAL_NUM);
        }
    }

    private void character() {
        if (peek() != '\'' && !isAtEnd()) {
            advance(); // Consume the character inside
            if (peek() == '\'') {
                advance(); // Consume the closing quote
                addToken(TokenType.LITERAL_CHAR);
                return;
            }
        }
        // If we reach here, it's a malformed character literal
        addToken(TokenType.UNKNOWN);
    }

    // --- Helper Methods ---

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        column++;
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, source.substring(start, current));
    }

    private void addToken(TokenType type, String text) {
        tokens.add(new Token(type, text, line, startColumn));
    }
}