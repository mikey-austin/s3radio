package net.jackiemclean;

public interface TrackStream {
    long play(Track track);

    void start();

    void stop();
}
