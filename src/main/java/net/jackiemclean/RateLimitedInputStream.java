package net.jackiemclean;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimitedInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitedInputStream.class);

    private final InputStream delegate;
    private final RateLimiter rateLimiter;
    private final AtomicLong bytesRead;

    public RateLimitedInputStream(InputStream delegate, RateLimiter rateLimiter) {
        this.delegate = delegate;
        this.rateLimiter = rateLimiter;
        this.bytesRead = new AtomicLong();
    }

    @Override
    public int read() throws IOException {
        rateLimiter.acquire(Integer.BYTES);
        int next = delegate.read();
        long soFar = bytesRead.addAndGet(Integer.BYTES);
        if (soFar % 4_000 == 0) {
            LOG.info("read {} bytes so far", soFar);
        }

        return next;
    }
}
