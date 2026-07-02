package org.hamisi.swoopdserver.common;

import org.hamisi.swoopdserver.common.exceptions.InvalidTokenException;
import org.hamisi.swoopdserver.common.exceptions.TokenServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidResponse(InvalidTokenException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(TokenServiceException.class)
    public ResponseEntity<String> handleTokenServiceError(TokenServiceException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
