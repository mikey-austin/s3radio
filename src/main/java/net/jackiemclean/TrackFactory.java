package net.jackiemclean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrackFactory {

    public Track create(String name, Station station, File trackFile) {
        return new Track(name, station, () -> {
            try {
                return new FileInputStream(trackFile);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }, trackFile.length());
    }

    public Track create(String name, Station station, InputStream content, long size) {
        return new Track(name, station, content, size);
    }
}
