int runLoops() {
    int i = 0, sum = 0;

    for (i = 0; i < 10; i = i + 1) {
        sum = sum + i;
    }

    while (sum > 0) {
        if (sum > 25) {
            sum = sum - 2;
        } else {
            sum = sum - 1;
        }
    }

    return sum;
}