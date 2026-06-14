package com.compiler;

import com.compiler.lexer.Lexer;
import com.compiler.lexer.Token;
import com.compiler.parser.Parser;
import com.compiler.parser.ParseTreeNode;
import com.compiler.semantic.SemanticAnalyzer;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Test Code designed to trigger our new relational and return type checks
        String sourceCode = """
                int x = 5;
                float y = 10.5;
                
                // 1. Valid loop: (int < int) evaluates to a valid integer conditional
                while (x < 20) {
                    x = x + 1;
                }
                
                // 2. Semantic Error: Relational mismatch (int < float) yields an UNKNOWN type
                while (x < y) {
                    x = x + 2;
                }
                
                // 3. Semantic Error: Return statement containing mixed math (int + float)
                return x + y;
                """;

        System.out.println("====== Starting Compilation ======");

        // [1/3] Lexical Analysis
        System.out.println("\n[1/3] Running Lexical Analysis...");
        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        // [2/3] Syntax Analysis
        System.out.println("[2/3] Running Syntax Analysis...");
        Parser parser = new Parser(tokens);
        ParseTreeNode root = parser.parse();

        // Output CST & Syntax Errors
        System.out.println("\n--- Concrete Syntax Tree ---");
        root.printTree();

        System.out.println("\n--- Syntax Error Log ---");
        List<String> syntaxErrors = parser.getErrorLog();
        if (syntaxErrors.isEmpty()) {
            System.out.println("0 Syntax Errors found.");
        } else {
            for (String error : syntaxErrors) {
                System.out.println(" - " + error);
            }
        }

        // [3/3] Semantic Analysis
        System.out.println("\n[3/3] Running Semantic Analysis...");
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(root);
        semanticAnalyzer.analyze();

        // Output Semantic Errors
        System.out.println("\n--- Semantic Error Log ---");
        List<String> semanticErrors = semanticAnalyzer.getSemanticErrors();
        if (semanticErrors.isEmpty()) {
            System.out.println("0 Semantic Errors found. Symbol Table verification successful.");
        } else {
            for (String error : semanticErrors) {
                System.out.println(" - " + error);
            }
        }

        System.out.println("\n====== Compilation Finished ======");
    }
}