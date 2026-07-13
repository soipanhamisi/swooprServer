package org.hamisi.swoopdserver.auth.exceptions;

public class StaleOtpException extends RuntimeException {
    public StaleOtpException(String couldNotVerifyUserIdentity) {
        super(couldNotVerifyUserIdentity);
    }
}
