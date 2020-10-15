package net.jackiemclean;

public interface Station extends Iterable<Track> {

    String getName();

    void start();

    void stop();
}
