package com.comercio.shared.exception;

/** Regla de negocio incumplida con una petición sintácticamente válida (p. ej. producto inactivo). */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "BUSINESS_RULE_VIOLATION";
    }
}
