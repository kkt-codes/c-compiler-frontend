/* Semantic Violation Trap
   Tests: Immutability, scope redeclaration, undeclared variables,
   and strict type mismatching.
*/

int computePenalty(int daysLate) {
    const int maxPenalty = 500;

    // 1. Redeclaration Error: Parameter name reused as local variable
    int daysLate = 5;

    // 2. Immutability Error: Attempting to reassign a constant
    maxPenalty = 600;

    // 3. Undeclared Identifier Error
    untrackedVariable = 10;

    int currentFine = 50;
    float interestRate = 1.25;

    // 4. Type Mismatch Error: Cannot assign evaluated float to int target
    currentFine = currentFine * interestRate;

    return currentFine;
}

void main() {
    // 5. Constant Initialization Error: Const declared without assignment
    const float strictLimit;
}