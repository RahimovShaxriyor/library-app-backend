package auth_service.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";


    public void blacklistToken(String token, Date expiryDate) {
        long remainingValidity = expiryDate.getTime() - System.currentTimeMillis();
        if (remainingValidity > 0) {
            String redisKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(redisKey, true, remainingValidity, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String redisKey = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(redisKey);
    }
}