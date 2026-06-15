int verifyAccess(int age, int overrideCode) {
    int grantAccess = 0, threshold = 65;

    if (age + 2 >= 18 && overrideCode == 999 || age > threshold) {
        grantAccess = 1;
        return grantAccess;
    } else {
        return grantAccess;
    }
}