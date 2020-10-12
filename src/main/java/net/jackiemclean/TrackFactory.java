package net.jackiemclean;

import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrackFactory {

    public Track create(String name, Station station, InputStream content, long size) {
        return new Track(name, station, content, size);
    }
}
