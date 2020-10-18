package net.jackiemclean;

import java.io.InputStream;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Track {

    public final String name;
    public final long sizeInBytes;
    public final Station station;
    public final Supplier<InputStream> contentSupplier;

    public Track(String name, Station station, Supplier<InputStream> contentSupplier, long size) {
        this.name = name;
        this.station = station;
        this.contentSupplier = contentSupplier;
        this.sizeInBytes = size;
    }

    public Track(String name, Station station, InputStream content, long size) {
        this(name, station, () -> content, size);
    }

    @Override
    public String toString() {
        return "Track [name=" + name + ", sizeInBytes=" + sizeInBytes + ", station=" + station + "]";
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonIgnore
    public Station getStation() {
        return station;
    }

    @JsonIgnore
    public InputStream getContent() {
        return contentSupplier.get();
    }

    @JsonProperty("sizeInBytes")
    public long getSizeInBytes() {
        return sizeInBytes;
    }
}
