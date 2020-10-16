package net.jackiemclean;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimitedOutputStream extends PipedOutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitedOutputStream.class);

    private final RateLimiter rateLimiter;
    private final AtomicLong bytesWritten;

    public RateLimitedOutputStream(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        this.bytesWritten = new AtomicLong();
    }

    @Override
    public void write(int arg0) throws IOException {
        LOG.info("writing {} bytes to output", 4);
        rateLimiter.acquire(Integer.BYTES);
        super.write(arg0);
        flush();

        long soFar = bytesWritten.addAndGet(Integer.BYTES);
        if (soFar % 4_000 == 0) {
            LOG.info("wrote {} bytes so far", soFar);
        }
    }
}
