package com.nationale.currency.acoount.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class AppExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return this.handleExceptionInternal(ex, errors, headers, status, request);
    }

    @ExceptionHandler({InsufficientFundsException.class, InvalidExchangeException.class,
            AccountNotFoundException.class, AccountNotFoundException.class, NbpApiException.class})
    public final ResponseEntity<Object> handleCustomExceptions(Exception ex, WebRequest request) {
        if (ex instanceof NbpApiException) {
            return this.handleExceptionInternal(ex, Map.of("message", "Cannot fetch the latest ratings from NBP API."),
                    new HttpHeaders(),
                    HttpStatus.INTERNAL_SERVER_ERROR, request);
        }

        return this.handleExceptionInternal(ex, Map.of("message", ex.getMessage()), new HttpHeaders(),
                HttpStatus.BAD_REQUEST, request);
    }
}
