package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.proxies.ResendProxy;
import org.hamisi.swoopdserver.auth.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UserAuthenticationService {
    private final ResendProxy resendProxy;
    private final OtpRepository otpRepository;

    public UserAuthenticationService(ResendProxy resendProxy, OtpRepository otpRepository) {
        this.resendProxy = resendProxy;
        this.otpRepository = otpRepository;
    }

    public void createOtp(String email){
        int otp = new Random().nextInt(899) + 100;
        otpRepository.saveOtp(email, otp);
        String firstName = email.substring(1, email.indexOf('@'));
        resendProxy.sendOTP(email, firstName, otp);
    }


    public boolean verifyOtp(int otp, String email){
        if (otpRepository.getOtp(email) == null){
            return false;
        }else if (otpRepository.getOtp(email).equals(String.valueOf(otp))){
            otpRepository.deleteOtp(email);
            return true;
        }else{
            return false;
        }
    }
}
