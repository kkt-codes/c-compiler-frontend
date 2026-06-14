int verifyAccess(int age, int overrideCode) {
    int grantAccess;
    grantAccess = 0;

    if (age >= 18 && overrideCode == 999 || age > 65) {
        grantAccess = 1;
        return grantAccess;
    } else {
        return grantAccess;
    }
}