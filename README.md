# C-Lite Compiler Frontend & Visual IDE

Welcome to the **C-Lite Compiler Frontend**, an interactive, JavaFX-powered desktop development environment that executes full compile-time validation on a structured subset of C-language source files.

This project implements a complete, decoupled three-stage compiler frontend pipeline-featuring a Lexical Analyzer (Lexer), a Syntax Analyzer (Parser) with predictive parsing and statement synchronization error recovery, an active Block-Scoped Symbol Table, and a type-synthesizing Semantic Analyzer.

---

## Key Features

### 1. Lexical Analysis (Scanner)
* **Robust Tokenization:** Safely converts raw text streams into discrete tokens, handling numeric literals (ints, floats, doubles), keywords, identifiers, and relational operators.
* **Fault Tolerance:** Gracefully registers invalid symbols as `UNKNOWN` tokens to prevent system crashes and entirely ignores multi-line and single-line comments.

### 2. Syntax Analysis (Parser)
* **Parse Tree:** Constructs a comprehensive Parse Tree using recursive descent, supporting complex function definitions, control loops (`for`, `while`), and selection structures (`if-else`).
* **Panic-Mode Error Recovery:** Catches structural anomalies, logs the syntax exception, and safely skips to the nearest statement delimiter (like a semicolon) to continue parsing without halting.

### 3. Semantic Analysis & Type Verification
* **Sequential Lookahead Architecture:** Cleanly processes complex flat-tree declarations, effortlessly validating sequential comma-separated variable chains (e.g., `int i = 0, sum = 0;`).
* **Active Scope Tracking:** Maintains a block-scoped environment using a stack-based Symbol Table to handle nested block levels, local parameter registers, and global fallbacks.
* **Strict Enforcement:** Blocks unauthorized mutations on `const` variables, intercepts implicit narrowing assignments, and dynamically handles widening type promotions (e.g., `int` -> `float` -> `double`).

### 4. Visual IDE Integration
* **Live Diagnostics:** A JavaFX-powered dashboard provides real-time tabular displays for the live Token Stream and the active Symbol Table ledger.
* **Tree Visualizer:** Renders the structural Parse Tree in an expandable UI hierarchy, stripping out redundant epsilon elements for clean, focused diagnostics.

---

## Getting Started

**Prerequisites:**
* Java JDK 25 or higher
* Maven

**Build & Run:**
You can launch the visual IDE directly from your terminal. Navigate to the project root directory and choose one of the execution methods below.

**Option 1: The Quick Launch** This command rapidly cleans the previous build environment, compiles the source code, and immediately spins up the JavaFX graphical interface.
```bash
mvn clean javafx:run
```

**Option 2: Full Build Pipeline** If you are running the project for the very first time or need to resolve dependencies, use this two-step process:
```bash
mvn clean compile
mvn javafx:run
```

---

## Testing Suite

The project includes a dedicated suite of 8 standard `.c` source files designed to trigger specific compiler traps and test valid execution. Load these directly into the UI:

**Valid Execution & Features:**
* **`test_comprehensive.c`:** Tests the full pipeline with widening coercion, boolean logic, and nested scopes.
* **`test_semantic_valid.c`:** A logistics freight calculator demonstrating complex float/int math and constant evaluation.
* **`test_logic.c`:** Validates complex relational (`>=`, `==`) and logical (`&&`, `||`) operator precedence.
* **`test_loops.c`:** Tests the structural integrity of nested `for` and `while` iterative statements.
* **`test_functions.c`:** Verifies parameter passing, scope entry/exit, and return statement evaluations.

**Error Handling & Traps:**
* **`test_lexical_errors.c`:** Flags illegal characters, malformed char literals, and unclosed comments.
* **`test_syntax_errors.c`:** Flags missing punctuation, stray operators, and unbalanced braces.
* **`test_semantic_errors.c`:** Flags scope redeclarations, immutability violations on `const` variables, and strict type mismatching.

---

## Project Structure

```text
compiler-frontend/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── compiler/
│       │           ├── exception/
│       │           │   ├── LexicalException.java
│       │           │   ├── SemanticException.java
│       │           │   └── SyntaxException.java
│       │           ├── lexer/
│       │           │   ├── Lexer.java
│       │           │   ├── Token.java
│       │           │   └── TokenType.java
│       │           ├── parser/
│       │           │   ├── Parser.java
│       │           │   └── ParseTreeNode.java
│       │           ├── semantic/
│       │           │   ├── SemanticAnalyzer.java
│       │           │   ├── Symbol.java
│       │           │   └── SymbolTable.java  
│       │           ├── ui/
│       │           │   └── AppController.java  
│       │           └── Main.java
│       └── resources/
│           ├── samples/
│           │   ├── test_comprehensive.c
│           │   ├── test_functions.c
│           │   ├── test_lexical_errors.c
│           │   ├── test_logic.c
│           │   ├── test_loops.c
│           │   ├── test_semantic_errors.c
│           │   ├── test_semantic_valid.c
│           │   └── test_syntax_errors.c
│           ├── layout.fxml
│           └── style.css
├── .gitignore
├── pom.xml
└── README.md
```