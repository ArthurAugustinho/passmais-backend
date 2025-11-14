package com.passmais.infrastructure.validation;

public final class CPFValidator {
    private CPFValidator() {}

    public static boolean isValid(String cpf) {
        if (cpf == null) return false;
        String num = cpf.replaceAll("\\D", "");
        if (num.length() != 11) return false;
        if (num.chars().distinct().count() == 1) return false; // all same digits
        try {
            int d1 = calcDigit(num.substring(0, 9));
            int d2 = calcDigit(num.substring(0, 9) + d1);
            return num.equals(num.substring(0, 9) + d1 + d2);
        } catch (Exception e) {
            return false;
        }
    }

    private static int calcDigit(String str) {
        int sum = 0;
        int weight = str.length() + 1;
        for (char c : str.toCharArray()) {
            sum += (c - '0') * weight--;
        }
        int mod = sum % 11;
        int res = 11 - mod;
        return (res > 9) ? 0 : res;
    }
}

