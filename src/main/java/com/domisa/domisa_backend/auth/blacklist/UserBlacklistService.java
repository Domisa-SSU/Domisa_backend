package com.domisa.domisa_backend.auth.blacklist;

import com.domisa.domisa_backend.auth.blacklist.entity.UserBlacklist;
import com.domisa.domisa_backend.auth.blacklist.repository.UserBlacklistRepository;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlacklistService {

	private static final String BLACKLISTED_VALUE = "true";
	private static final String NOT_BLACKLISTED_VALUE = "false";

	private final StringRedisTemplate stringRedisTemplate;
	private final UserBlacklistRepository userBlacklistRepository;
	private final UserRepository userRepository;

	@Value("${app.blacklist.redis-key-prefix:blacklist:user:}")
	private String redisKeyPrefix;

	@Value("${app.blacklist.not-blacklisted-cache-ttl:PT5M}")
	private Duration notBlacklistedCacheTtl;

	@Transactional(readOnly = true)
	public boolean isBlacklisted(Long userId) {
		if (userId == null) {
			return false;
		}

		String key = buildKey(userId);
		String cachedValue = stringRedisTemplate.opsForValue().get(key);
		if (BLACKLISTED_VALUE.equals(cachedValue)) {
			return true;
		}
		if (NOT_BLACKLISTED_VALUE.equals(cachedValue)) {
			return false;
		}

		boolean blacklisted = userBlacklistRepository.existsByUserId(userId);
		cacheBlacklistStatus(key, blacklisted);
		return blacklisted;
	}

	@Transactional
	public void blacklist(Long userId, String reason) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		if (userBlacklistRepository.findByUserId(userId).isEmpty()) {
			userBlacklistRepository.save(UserBlacklist.create(user));
		}
		stringRedisTemplate.opsForValue().set(buildKey(userId), BLACKLISTED_VALUE);
	}

	@Transactional
	public void blacklist(Long userId) {
		blacklist(userId, null);
	}

	@Transactional
	public void removeBlacklist(Long userId) {
		userBlacklistRepository.deleteByUserId(userId);
		cacheBlacklistStatus(buildKey(userId), false);
	}

	public String buildKey(Long userId) {
		return redisKeyPrefix + userId;
	}

	private void cacheBlacklistStatus(String key, boolean blacklisted) {
		if (blacklisted) {
			stringRedisTemplate.opsForValue().set(key, BLACKLISTED_VALUE);
			return;
		}
		stringRedisTemplate.opsForValue().set(key, NOT_BLACKLISTED_VALUE, notBlacklistedCacheTtl);
	}
}
