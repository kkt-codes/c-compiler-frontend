float computeDiscount(float price, int itemQuantity) {
    float finalPrice = 0.0;

    if (itemQuantity > 5 && price > 50.0) {
        finalPrice = price - 10.5;
    } else {
        finalPrice = price;
    }

    /* This is a multi-line comment.
           The Lexer should completely ignore these words:
           int float while if && || { }
    */

    return finalPrice;
}



int main() {
    int loopCounter = 0, batchSize = 4;
    float baseCost = 60.0, runningTotal = 0.0;

    for (loopCounter = 0; loopCounter < batchSize; loopCounter = loopCounter + 1) {
        runningTotal = runningTotal + baseCost;
    }

    float finalReceipt;
    finalReceipt = computeDiscount(runningTotal, batchSize);

    return 0;
}