package org.hamisi.swoopdserver.auth.controller;

import org.hamisi.swoopdserver.auth.exceptions.InvalidEmailException;
import org.hamisi.swoopdserver.auth.exceptions.NoUserWithMatchingEmailException;
import org.hamisi.swoopdserver.auth.exceptions.StaleOtpException;
import org.hamisi.swoopdserver.auth.exceptions.UserExistsException;
import org.hamisi.swoopdserver.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception responses for the authentication controller.
 *
 * <p>Most handlers return the shared {@code ApiResponse} envelope:</p>
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Reason for failure",
 *   "data": null
 * }
 * }</pre>
 */
@RestControllerAdvice
public class AuthControllerExceptionHandlers {

    /**
     * Returned when the supplied email does not match any registered user.
     *
     * <p>Current outbound body shape: plain string response, not an {@code ApiResponse} object.</p>
     * <pre>{@code
     * "<error message>, Go to Registration"
     * }</pre>
     */
    @ExceptionHandler(NoUserWithMatchingEmailException.class)
    public ResponseEntity<String> handleUserEmailNotFound(NoUserWithMatchingEmailException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()+", Go to Registration");
    }

    /**
     * Returned when an email fails validation.
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": false,
     *   "message": "<validation message>",
     *   "data": null
     * }
     * }</pre>
     */
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiResponse<Void>>handleInvalidEmail(InvalidEmailException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when a user attempts to register with an email that already exists.
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": false,
     *   "message": "<duplicate-user message>",
     *   "data": null
     * }
     * }</pre>
     */
    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserExists(UserExistsException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * Returned when the provided OTP is stale or expired.
     *
     * <p>Outbound JSON:</p>
     * <pre>{@code
     * {
     *   "success": false,
     *   "message": "<stale-otp message>",
     *   "data": null
     * }
     * }</pre>
     */
    @ExceptionHandler(StaleOtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleStaleOtp(StaleOtpException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(ex.getMessage()));
    }
}
