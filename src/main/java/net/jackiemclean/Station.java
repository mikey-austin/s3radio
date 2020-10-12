package net.jackiemclean;

public interface Station extends Iterable<Track> {
    void start();

    void stop();
}
