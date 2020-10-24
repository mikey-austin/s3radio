package net.jackiemclean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.InputStream;
import java.util.function.Supplier;

public class Track {

    private final String name;
    private final long sizeInBytes;
    private final Station station;
    private final Supplier<InputStream> contentSupplier;

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
        return "Track [name="
                + name
                + ", sizeInBytes="
                + sizeInBytes
                + ", station="
                + station
                + "]";
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonIgnore
    public boolean isStandby() {
        return name.matches("^standby");
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
