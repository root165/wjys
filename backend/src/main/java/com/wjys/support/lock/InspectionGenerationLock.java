package com.wjys.support.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionGenerationLock {
    private static final String KEY = "inspection:generate";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${wjys.redis.enabled:false}")
    private boolean redisEnabled;

    public String tryAcquire() {
        if (!redisEnabled) return "local-noop-lock";
        String token = UUID.randomUUID().toString();
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(KEY, token, Duration.ofMinutes(5))) ? token : null;
        } catch (RuntimeException exception) {
            log.error("巡检任务调度锁不可用，跳过本轮生成", exception);
            return null;
        }
    }

    public void release(String token) {
        if (!redisEnabled || token == null || "local-noop-lock".equals(token)) return;
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(KEY), token);
        } catch (RuntimeException exception) {
            log.warn("释放巡检任务调度锁失败", exception);
        }
    }
}
