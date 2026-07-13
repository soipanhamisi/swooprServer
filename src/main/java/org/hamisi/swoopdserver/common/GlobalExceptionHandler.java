package org.hamisi.swoopdserver.common;

import org.hamisi.swoopdserver.common.exceptions.InvalidTokenException;
import org.hamisi.swoopdserver.common.exceptions.TokenServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TokenServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorsInTokenService(TokenServiceException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }
}
