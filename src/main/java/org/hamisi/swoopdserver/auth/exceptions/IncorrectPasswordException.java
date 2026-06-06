package org.hamisi.swoopdserver.auth.exceptions;

public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException(String incorrectPassword) {
        super(incorrectPassword);
    }
}
