package br.com.descontovivo.shared.api;

public record ApiErrorResponse(int status, String error, String message) {}
