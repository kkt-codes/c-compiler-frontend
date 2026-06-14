package com.compiler.ui;

import com.compiler.exception.LexicalException;
import com.compiler.exception.SyntaxException;
import com.compiler.exception.SemanticException;
import com.compiler.lexer.Lexer;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;
import com.compiler.parser.Parser;
import com.compiler.parser.ParseTreeNode;
import com.compiler.semantic.SemanticAnalyzer;
import com.compiler.semantic.Symbol;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AppController {

    @FXML private TextArea codeTextArea;
    @FXML private TextArea consoleTextArea;

    // Token Presentation Elements
    @FXML private TableView<Token> tokenTableView;
    @FXML private TableColumn<Token, TokenType> tokenTypeColumn;
    @FXML private TableColumn<Token, String> tokenLexemeColumn;
    @FXML private TableColumn<Token, Integer> tokenLineColumn;
    @FXML private TableColumn<Token, Integer> tokenColumnColumn;

    // Ast View Components
    @FXML private TreeView<String> parseTreeView;

    // Symbol View Table Elements
    @FXML private TableView<Symbol> symbolTableView;
    @FXML private TableColumn<Symbol, String> symbolNameColumn;
    @FXML private TableColumn<Symbol, TokenType> symbolTypeColumn;
    @FXML private TableColumn<Symbol, Integer> symbolScopeColumn;

    @FXML
    public void initialize() {
        tokenTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        tokenLexemeColumn.setCellValueFactory(new PropertyValueFactory<>("lexeme"));
        tokenLineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        tokenColumnColumn.setCellValueFactory(new PropertyValueFactory<>("column"));

        symbolNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbolTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        symbolScopeColumn.setCellValueFactory(new PropertyValueFactory<>("scopeLevel"));
    }

    @FXML
    public void handleCompile() {
        // Clear old runs
        consoleTextArea.clear();
        tokenTableView.getItems().clear();
        parseTreeView.setRoot(null);
        symbolTableView.getItems().clear();

        String rawSource = codeTextArea.getText();
        if (rawSource == null || rawSource.trim().isEmpty()) {
            consoleTextArea.setText("System: Source code container is completely empty.");
            return;
        }

        // --- 1. LEXICAL RUN ---
        Lexer lexer = new Lexer(rawSource);
        List<Token> tokens = lexer.tokenize();
        tokenTableView.setItems(FXCollections.observableArrayList(tokens));

        if (!lexer.getErrorLog().isEmpty()) {
            consoleTextArea.appendText(">>> Lexical Scanning Failures Raised <<<\n");
            for (LexicalException lexEx : lexer.getErrorLog()) {
                consoleTextArea.appendText(lexEx.getMessage() + "\n");
            }
            return;
        }

        // --- 2. SYNTAX PARSE RUN ---
        Parser parser = new Parser(tokens);
        ParseTreeNode programRoot = parser.parse();

        if (!parser.getErrorLog().isEmpty()) {
            consoleTextArea.appendText(">>> Syntax Parse Errors Raised <<<\n");
            for (SyntaxException synEx : parser.getErrorLog()) {
                consoleTextArea.appendText(synEx.getMessage() + "\n");
            }
        }

        // Populate tree representation safely to help with diagnostics
        if (programRoot != null) {
            TreeItem<String> generatedRoot = buildVisualTree(programRoot);
            if (generatedRoot != null) {
                parseTreeView.setRoot(generatedRoot);
                generatedRoot.setExpanded(true);
            }
        }

        if (!parser.getErrorLog().isEmpty()) {
            return;
        }

        // --- 3. SEMANTIC SCOPE RUN ---
        SemanticAnalyzer analyzer = new SemanticAnalyzer(programRoot);
        analyzer.analyze();

        symbolTableView.setItems(FXCollections.observableArrayList(analyzer.getSymbolHistory()));

        if (!analyzer.getErrorLog().isEmpty()) {
            consoleTextArea.appendText(">>> Semantic Violations Raised <<<\n");
            for (SemanticException semEx : analyzer.getErrorLog()) {
                consoleTextArea.appendText(semEx.getMessage() + "\n");
            }
        } else {
            consoleTextArea.appendText("Compilation Finished Successfully! 0 frontend anomalies detected.\n");
        }
    }

    @FXML
    public void handleClearCode() {
        codeTextArea.clear();
        consoleTextArea.clear();
        tokenTableView.getItems().clear();
        parseTreeView.setRoot(null);
        symbolTableView.getItems().clear();
    }

    private TreeItem<String> buildVisualTree(ParseTreeNode node) {
        if (node == null) return null;

        // Filter out structural tokens representing epsilon leaves cleanly
        if (node.getNodeName().equals("ε") || node.getNodeName().equals("蔚")) {
            return null;
        }

        // Clean redundant empty sub-chains containing tail nodes
        if (node.getNodeName().endsWith("Prime") || node.getNodeName().endsWith("Tail")) {
            if (node.getChildren().size() == 1) {
                String singleChild = node.getChildren().get(0).getNodeName();
                if (singleChild.equals("ε") || singleChild.equals("蔚")) {
                    return null;
                }
            }
        }

        String displayLabel = (node.getToken() != null)
                ? String.format("%s: %s", node.getToken().getType(), node.getToken().getLexeme())
                : node.getNodeName();

        TreeItem<String> itemNode = new TreeItem<>(displayLabel);

        for (ParseTreeNode child : node.getChildren()) {
            TreeItem<String> processedChild = buildVisualTree(child);
            if (processedChild != null) {
                itemNode.getChildren().add(processedChild);
            }
        }

        return itemNode;
    }
}