package net.jackiemclean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import com.google.common.util.concurrent.RateLimiter;

@Dependent
public class RateLimiterFactory {

    private static final long BYTES_PER_SECOND = 4_096;

    @Produces
    public RateLimiter create() {
        return RateLimiter.create(BYTES_PER_SECOND);
    }
}
