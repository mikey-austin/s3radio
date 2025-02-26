package net.jackiemclean;

import com.google.common.util.concurrent.RateLimiter;

import java.io.InputStream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@Dependent
public class RateLimitedStreamFactory {

    private final RateLimiter rateLimiter;

    @Inject
    public RateLimitedStreamFactory(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public RateLimitedInputStream limitInputStream(InputStream delegate) {
        return new RateLimitedInputStream(delegate, rateLimiter);
    }

    @Produces
    public RateLimitedOutputStream create() {
        return new RateLimitedOutputStream(rateLimiter);
    }
}
