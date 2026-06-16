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

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AppController {

    @FXML private TextArea codeTextArea;
    @FXML private TextArea consoleTextArea;

    // Token Presentation Elements
    @FXML private TableView<Token> tokenTableView;
    @FXML private TableColumn<Token, TokenType> tokenTypeColumn;
    @FXML private TableColumn<Token, String> tokenLexemeColumn;
    @FXML private TableColumn<Token, Integer> tokenLineColumn;
    @FXML private TableColumn<Token, Integer> tokenColumnColumn;

    // Parse Tree View Components
    @FXML private TreeView<String> parseTreeView;

    // Symbol View Table Elements
    @FXML private TableView<Symbol> symbolTableView;
    @FXML private TableColumn<Symbol, String> symbolNameColumn;
    @FXML private TableColumn<Symbol, String> symbolTypeColumn;
    @FXML private TableColumn<Symbol, String> symbolCategoryColumn;
    @FXML private TableColumn<Symbol, Integer> symbolScopeColumn;
    @FXML private TableColumn<Symbol, Integer> symbolLineColumn;

    @FXML
    public void initialize() {
        tokenTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        tokenLexemeColumn.setCellValueFactory(new PropertyValueFactory<>("lexeme"));
        tokenLineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        tokenColumnColumn.setCellValueFactory(new PropertyValueFactory<>("column"));

        symbolNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbolTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        symbolCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        symbolScopeColumn.setCellValueFactory(new PropertyValueFactory<>("scope"));
        symbolLineColumn.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));

        String starterCode = """
            int main() {
                // This comment will be ignored.
                int x = 10;
                float y = 5.5;
                
                if (x > 5) {
                    y = y + 1.0;
                }
                
                return 0;
            }
            """;
        codeTextArea.setText(starterCode);
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
            consoleTextArea.appendText("Compilation Finished Successfully! 0(Zero) frontend anomalies detected.\n");
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

    @FXML
    public void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open C Source File");

        // Restrict the file picker to standard C and text files
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("C Source Files", "*.c"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) codeTextArea.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Read the entire file into a single string and push it to the UI
                String content = Files.readString(selectedFile.toPath());
                codeTextArea.setText(content);
                consoleTextArea.setText("System: Successfully loaded file -> " + selectedFile.getName() + "\n");
            } catch (IOException e) {
                consoleTextArea.setText("System Error: Could not read the file.\n" + e.getMessage());
            }
        } else {
            consoleTextArea.setText("System: File load cancelled by user.\n");
        }
    }

    private TreeItem<String> buildVisualTree(ParseTreeNode node) {
        if (node == null) return null;

        // Filter out structural tokens representing epsilon leaves cleanly
        if (node.getNodeName().equals("ε")) {
            return null;
        }

        // Clean redundant empty sub-chains containing tail nodes
        if (node.getNodeName().endsWith("Prime") || node.getNodeName().endsWith("Tail")) {
            if (node.getChildren().size() == 1) {
                String singleChild = node.getChildren().get(0).getNodeName();
                if (singleChild.equals("ε")) {
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