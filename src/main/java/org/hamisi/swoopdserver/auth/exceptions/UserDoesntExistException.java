package org.hamisi.swoopdserver.auth.exceptions;

public class UserDoesntExistException extends RuntimeException {
    public UserDoesntExistException(String userDoesntExist) {
        super(userDoesntExist);
    }
}
