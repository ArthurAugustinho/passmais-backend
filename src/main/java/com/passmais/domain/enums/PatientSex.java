package com.passmais.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Locale;

public enum PatientSex {
    MASCULINO("Masculino"),
    FEMININO("Feminino"),
    NAO_INFORMADO("Não informado");

    private final String display;

    PatientSex(String display) {
        this.display = display;
    }

    @JsonValue
    public String getDisplay() {
        return display;
    }

    @JsonCreator
    public static PatientSex fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MASCULINO" -> MASCULINO;
            case "FEMININO" -> FEMININO;
            case "NAOINFORMADO" -> NAO_INFORMADO;
            default -> throw new IllegalArgumentException("Valor inválido para sexo: " + value);
        };
    }
}
