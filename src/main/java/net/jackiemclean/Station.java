package net.jackiemclean;

import java.util.Optional;

public interface Station {

    String getName();

    Optional<TrackStream> getTrackStream();

    void start();

    void stop();

    void standbyOn();

    void standbyOff();

    Optional<String> getStreamUrl();
}
