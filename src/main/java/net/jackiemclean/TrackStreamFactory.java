package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TrackStreamFactory {
    public TrackStream createStream(Station station) {
        return new TrackStream() {
            @Override
            public long play(Track track) {
                LoggerFactory.getLogger(TrackStream.class).info("playing track: {}", track);
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                }
                return 0L;
            }

            @Override
            public void start() {
                LoggerFactory.getLogger(TrackStream.class).info("starting track stream for station: {}", station);
            }

            @Override
            public void stop() {
                LoggerFactory.getLogger(TrackStream.class).info("stopping track stream for station: {}", station);

            }
        };
    }
}
