package org.hamisi.swoopdserver.auth.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OtpRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public OtpRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveOtp(String email, int otp) {
        redisTemplate.opsForValue().set("OTP:" + email, String.valueOf(otp), 5, java.util.concurrent.TimeUnit.MINUTES);
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
