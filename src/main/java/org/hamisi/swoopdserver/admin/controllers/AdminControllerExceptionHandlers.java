package org.hamisi.swoopdserver.admin.controllers;

import org.hamisi.swoopdserver.admin.exceptions.AdminAccessDeniedException;
import org.hamisi.swoopdserver.admin.exceptions.AdminLoginException;
import org.hamisi.swoopdserver.admin.exceptions.AdminOperationException;
import org.hamisi.swoopdserver.admin.exceptions.AdminResourceNotFoundException;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminControllerExceptionHandlers {

    @ExceptionHandler(AdminLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminLoginException(AdminLoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminAccessDenied(AdminAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AdminOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminOperationException(AdminOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AdminResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminResourceNotFound(AdminResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(ex.getMessage()));
    }
}

