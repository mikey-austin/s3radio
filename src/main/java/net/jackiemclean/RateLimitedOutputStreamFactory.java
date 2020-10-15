package net.jackiemclean;

import java.io.PipedOutputStream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.google.common.util.concurrent.RateLimiter;

@Dependent
public class RateLimitedOutputStreamFactory {

    private final RateLimiter rateLimiter;

    @Inject
    public RateLimitedOutputStreamFactory(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Produces
    public RateLimitedOutputStream create() {
        return new RateLimitedOutputStream(new PipedOutputStream(), rateLimiter);
    }
}
