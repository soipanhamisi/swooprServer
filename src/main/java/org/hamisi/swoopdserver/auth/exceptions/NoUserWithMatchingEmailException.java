package org.hamisi.swoopdserver.auth.exceptions;

public class NoUserWithMatchingEmailException extends RuntimeException {
    public NoUserWithMatchingEmailException(String s) {
        super(s);
    }
}
