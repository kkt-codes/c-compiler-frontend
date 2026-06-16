/* Syntax Violation Trap
   Tests: Missing punctuation, malformed control flow structures,
   and unbalanced delimiters.
*/

int computeTotal(int a, int b) {
    // 1. Missing Semicolon Error
    int sum = a + b
    int padding = 10;

    // 2. Stray/Malformed Operator Expression
    int result = a + * b;

    return sum + padding;
}

void main() {
    int x = 10;
    int i = 0;

    // 3. Missing Parentheses in Selection Statement Control Flow
    if x > 5 {
        x = x - 1;
    }

    // 4. Malformed Iterative Header (Missing components/extra semicolons)
    for (i = 0; i < 10) {
        x = x + 1;
    }

    // 5. Unbalanced Block Delimiter (Missing the closing function brace for main)
    return 0;