package com.passmais.interfaces.handler;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Aplica tratamento básico de dados de entrada em toda a API.
 * - Trim em todos os campos String (converte strings vazias para null)
 */
@ControllerAdvice
public class GlobalBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Remove espaços em branco das extremidades e converte vazio para null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}

