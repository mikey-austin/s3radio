package net.jackiemclean;

import java.io.IOException;
import java.util.Optional;

public interface TrackStream {
    long play(Track track) throws IOException;

    void start() throws IOException;

    void stop() throws IOException;

    int getPercentPlayed();

    Optional<Track> getNowPlaying();
}
