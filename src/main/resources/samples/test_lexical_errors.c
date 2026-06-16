/* Lexical Violation Trap
   Tests: Illegal characters, malformed literals, and unclosed structures.
*/

int main() {
    int validVar = 100;

    // 1. Illegal Characters (Should trigger Lexical UNKNOWN errors)
    int budget = 5000 $;
    float progress = 75.5 @;

    // 2. Malformed Character Literals (More than one character inside single quotes)
    char grade = 'ABC';

    // 3. Invalid Numeric Format Tokenization (If applicable to your lexer)
    int invalidHex = 0xG12;

    return 0;
}

/* 4. Unclosed Multi-line Comment Error
   The compiler should flag this because it runs directly into the End-Of-File (EOF)
   without finding the matching closing token.
int standardTrailingBlock() {
    return 1;
}