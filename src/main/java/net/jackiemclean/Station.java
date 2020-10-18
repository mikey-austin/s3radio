package net.jackiemclean;

import java.util.Optional;

public interface Station extends Iterable<Track> {

    String getName();

    Optional<TrackStream> getTrackStream();

    void start();

    void stop();
}
