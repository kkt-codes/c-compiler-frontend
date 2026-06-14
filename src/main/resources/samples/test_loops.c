void runLoops() {
    int i, sum;
    sum = 0;

    for (i = 0; i < 10; i = i + 1) {
        sum = sum + i;
    }

    while (sum > 0) {
        sum = sum - 1;
    }
}