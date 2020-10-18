package net.jackiemclean;

import java.util.Collection;
import java.util.Optional;

public interface Radio {
    Collection<Station> getStations();

    Optional<Station> getStation(String name);
}
