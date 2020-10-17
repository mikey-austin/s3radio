package net.jackiemclean;

import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Track {

    @JsonProperty
    public final String name;
    @JsonProperty
    public final long sizeInBytes;
    @JsonIgnore
    public final Station station;
    @JsonIgnore
    public final InputStream content;

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

    public String getName() {
        return name;
    }

    public Station getStation() {
        return station;
    }

    public InputStream getContent() {
        return content;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }
}
