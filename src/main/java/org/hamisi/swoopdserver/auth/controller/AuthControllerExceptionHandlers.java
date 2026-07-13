package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.exceptions.InvalidEmailException;
import org.hamisi.swoopdserver.auth.exceptions.NoUserWithMatchingEmailException;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthControllerExceptionHandlers {
    @ExceptionHandler(NoUserWithMatchingEmailException.class)
    public ResponseEntity<String> handleUserEmailNotFound(NoUserWithMatchingEmailException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()+", Go to Registration");
    }
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiResponse<Void>>handleInvalidEmail(InvalidEmailException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }
    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserExists(UserExistsException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }
}
