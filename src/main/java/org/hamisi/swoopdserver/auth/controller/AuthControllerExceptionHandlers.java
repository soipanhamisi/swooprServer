package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.common.exceptions.NoUserWithThatEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthControllerExceptionHandlers {
    @ExceptionHandler(NoUserWithThatEmailException.class)
    public ResponseEntity<String> handleUserEmailNotFound(NoUserWithThatEmailException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()+", Go to Registration");
    }
}
