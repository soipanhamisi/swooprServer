package org.hamisi.swoopdserver.auth.exceptions;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String notAValidUsiuEmail) {
        super(notAValidUsiuEmail);
    }
}
