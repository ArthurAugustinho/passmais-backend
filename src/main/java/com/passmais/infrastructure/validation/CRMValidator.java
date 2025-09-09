package com.passmais.infrastructure.validation;

import java.util.regex.Pattern;

public final class CRMValidator {
    private CRMValidator() {}

    private static final Pattern SIMPLE = Pattern.compile("^[0-9]{4,10}$");
    private static final Pattern WITH_UF = Pattern.compile("^[A-Z]{2}-[0-9]{4,10}$");

    public static boolean isValid(String crm) {
        if (crm == null) return false;
        String v = crm.trim().toUpperCase();
        return SIMPLE.matcher(v).matches() || WITH_UF.matcher(v).matches();
    }
}

