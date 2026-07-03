package org.hamisi.swoopdserver.common.exceptions;

public class NoUserWithThatEmailException extends RuntimeException {
    public NoUserWithThatEmailException(String s) {
        super(s);
    }
}
