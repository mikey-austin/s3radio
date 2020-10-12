package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TrackStreamerFactory {
    public TrackStream createStream(Track track) {
        return () -> {
            LoggerFactory.getLogger(TrackStream.class).info("playing track: {}", track);
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
            }
        };
    }
}
