package net.jackiemclean;

import java.io.InputStream;

public class Track {

    public final String name;
    public final Station station;
    public final InputStream content;
    public final long sizeInBytes;

    public Track(String name, Station station, InputStream content, long size) {
        this.name = name;
        this.station = station;
        this.content = content;
        this.sizeInBytes = size;
    }

    @Override
    public String toString() {
        return "Track [name=" + name + ", sizeInBytes=" + sizeInBytes + ", station=" + station + "]";
    }
}
