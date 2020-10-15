package net.jackiemclean;

import java.io.IOException;
import java.io.PipedOutputStream;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimitedOutputStream extends PipedOutputStream {

    private final PipedOutputStream delegate;
    private final RateLimiter rateLimiter;

    public RateLimitedOutputStream(PipedOutputStream delegate, RateLimiter rateLimiter) {
        this.delegate = delegate;
        this.rateLimiter = rateLimiter;
    }

    public PipedOutputStream getDelegate() {
        return delegate;
    }

    @Override
    public void write(int arg0) throws IOException {
        rateLimiter.acquire(Integer.BYTES);
        delegate.write(arg0);
    }
}
