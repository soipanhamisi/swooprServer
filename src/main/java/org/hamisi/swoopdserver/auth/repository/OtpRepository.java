package org.hamisi.swoopdserver.auth.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class OtpRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public OtpRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveOtp(String email, int otp) {
        redisTemplate.opsForValue().set("OTP:" + email, String.valueOf(otp), 45, TimeUnit.SECONDS);
    }

    public String getOtp(String email) {
        return redisTemplate.opsForValue().get("OTP:" + email);
    }

    public void deleteOtp(String email) {
        redisTemplate.delete("OTP:" + email);
    }

    public boolean exists(int otp) {
        String targetOtp = String.valueOf(otp);
        java.util.Set<String> keys = redisTemplate.keys("OTP:*");

        if (keys == null || keys.isEmpty()) {
            return false;
        }

        for (String key : keys) {
            String storedOtp = redisTemplate.opsForValue().get(key);
            if (targetOtp.equals(storedOtp)) {
                return true;
            }
        }

        return false;
    }
}
