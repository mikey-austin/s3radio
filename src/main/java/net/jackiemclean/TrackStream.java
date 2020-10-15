package net.jackiemclean;

import java.io.IOException;

public interface TrackStream {
    long play(Track track) throws IOException;

    void start() throws IOException;

    void stop() throws IOException;
}
