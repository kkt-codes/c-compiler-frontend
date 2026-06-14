package com.compiler.lexer;

import com.compiler.exception.LexicalException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<LexicalException> errorLog = new ArrayList<>();

    // Scanning offsets
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

    public List<LexicalException> getErrorLog() {
        return errorLog;
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
            case '/':
                if (match('/')) {
                    // Line Comments handler
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.DIV);
                }
                break;
            case '=':
                addToken(match('=') ? TokenType.EQ : TokenType.ASSIGN);
                break;
            case '!':
                if (match('=')) {
                    addToken(TokenType.NEQ);
                } else {
                    addToken(TokenType.UNKNOWN);
                }
                break;
            case '<':
                addToken(match('=') ? TokenType.LTE : TokenType.LT);
                break;
            case '>':
                addToken(match('=') ? TokenType.GTE : TokenType.GT);
                break;
            case ' ':
            case '\r':
            case '\t':
                // Skip basic whitespaces
                break;
            case '\n':
                line++;
                column = 1;
                break;
            case '\'':
                scanCharLiteral();
                break;
            default:
                if (isDigit(c)) {
                    scanNumberLiteral();
                } else if (isAlpha(c)) {
                    scanIdentifierOrKeyword();
                } else {
                    addToken(TokenType.UNKNOWN);
                }
                break;
        }
    }

    private void scanCharLiteral() {
        if (!isAtEnd() && peek() != '\'') {
            advance(); // Consume internal char character
            if (peek() == '\'') {
                advance(); // Consume terminal quote
                addToken(TokenType.LITERAL_CHAR);
                return;
            }
        } else if (!isAtEnd() && peek() == '\'') {
            advance(); // Handle empty sequences safely
            addToken(TokenType.LITERAL_CHAR);
            return;
        }
        addToken(TokenType.UNKNOWN);
    }

    private void scanNumberLiteral() {
        while (isDigit(peek())) advance();

        // Optional dot lookahead for tracking floating point tokens
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(TokenType.LITERAL_NUM);
    }

    private void scanIdentifierOrKeyword() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        addToken(type);
    }

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
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
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
        if (type == TokenType.UNKNOWN) {
            errorLog.add(new LexicalException("Invalid or unrecognized character sequence", line, startColumn, text));
        }
    }
}