package net.jackiemclean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import com.google.common.util.concurrent.RateLimiter;

@Dependent
public class RateLimiterFactory {

    // 30 x 32Kb frames per second for ogg.
    private static final long BYTES_PER_SECOND = 32_000 * 30;

    @Produces
    public RateLimiter create() {
        return RateLimiter.create(BYTES_PER_SECOND);
    }
}
