package org.hamisi.swoopdserver.common;

import org.hamisi.swoopdserver.common.exceptions.InvalidTokenException;
import org.hamisi.swoopdserver.common.exceptions.TokenServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception responses for shared token-management concerns.
 *
 * <p>All responses use the shared {@code ApiResponse} failure envelope:</p>
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Reason for failure",
 *   "data": null
 * }
 * }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Returned when the token service fails to extract or validate token data.
     */
    @ExceptionHandler(TokenServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorsInTokenService(TokenServiceException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when the supplied access token is invalid.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }
}
