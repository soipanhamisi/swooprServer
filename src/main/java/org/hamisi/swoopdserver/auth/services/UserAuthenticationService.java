package org.hamisi.swoopdserver.auth.services;

import org.hamisi.swoopdserver.auth.exceptions.InvalidEmailException;
import org.hamisi.swoopdserver.auth.proxies.ResendProxy;
import org.hamisi.swoopdserver.auth.repository.OtpRepository;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.hamisi.swoopdserver.auth.exceptions.NoUserWithMatchingEmailException;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class UserAuthenticationService {
    private final ResendProxy resendProxy;
    private final OtpRepository otpRepository;
    private final UsersRepository usersRepository;
    private final TokenManagementService tokenManagementService;

    public UserAuthenticationService(ResendProxy resendProxy, OtpRepository otpRepository, UsersRepository usersRepository, TokenManagementService tokenManagementService) {
        this.resendProxy = resendProxy;
        this.otpRepository = otpRepository;
        this.usersRepository = usersRepository;
        this.tokenManagementService = tokenManagementService;
    }

    public void createOtp(String email){
        if(!email.endsWith("@usiu.ac.ke")){
            throw new InvalidEmailException("Not a valid usiu email");
        }
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


    public String getNewToken(String email) {
        UUID userId = usersRepository.findUserIdByEmail(email);
        return tokenManagementService.createToken(userId, email);
    }


}
