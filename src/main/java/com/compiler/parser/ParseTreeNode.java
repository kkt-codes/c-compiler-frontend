package com.compiler.parser;

import com.compiler.lexer.Token;
import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {
    private final String nodeName;           // For Non-Terminals (e.g., "Expression")
    private final Token token;               // For Terminals (e.g., the actual '+' token)
    private final List<ParseTreeNode> children;

    // Constructor for Non-Terminal Nodes
    public ParseTreeNode(String nodeName) {
        this.nodeName = nodeName;
        this.token = null;
        this.children = new ArrayList<>();
    }

    // Constructor for Terminal Nodes (Leaves)
    public ParseTreeNode(Token token) {
        this.nodeName = token.getType().name();
        this.token = token;
        this.children = new ArrayList<>();
    }

    // Constructor for Epsilon (ε) Nodes
    public ParseTreeNode(String nodeName, boolean isEpsilon) {
        this.nodeName = isEpsilon ? "ε" : nodeName;
        this.token = null;
        this.children = new ArrayList<>();
    }

    public void addChild(ParseTreeNode child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public Token getToken() {
        return token;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }

    // --- Console Testing Utility ---

    public void printTree() {
        printTree(this, "", true);
    }

    private void printTree(ParseTreeNode node, String indent, boolean isLast) {
        System.out.print(indent);
        if (isLast) {
            System.out.print("└── ");
            indent += "    ";
        } else {
            System.out.print("├── ");
            indent += "│   ";
        }

        if (node.token != null) {
            System.out.println(node.token.getLexeme() + " [" + node.token.getType() + "]");
        } else {
            System.out.println(node.nodeName);
        }

        for (int i = 0; i < node.children.size(); i++) {
            printTree(node.children.get(i), indent, i == node.children.size() - 1);
        }
    }
}