package com.dentruth.common.util;

import java.time.Duration;

public interface RateLimiter {
    boolean tryAcquire(String key, int maxCount, Duration window);
}
