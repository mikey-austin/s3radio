package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TrackStreamFactory {

    private final String icecastBaseUri;
    private final String sourcePassword;
    private final String libshoutPath;

    @Inject
    public TrackStreamFactory(@ConfigProperty(name = "icecast.baseUri") String icecastBaseUri,
            @ConfigProperty(name = "icecast.password") String sourcePassword,
            @ConfigProperty(name = "icecast.libshoutPath", defaultValue = "/tmp/libshout-java.so") String libshoutPath) {
        this.icecastBaseUri = icecastBaseUri;
        this.sourcePassword = sourcePassword;
        this.libshoutPath = libshoutPath;
    }

    public TrackStream createStream(Station station) {
        return new LibshoutTrackStream(station.getName(), icecastBaseUri, sourcePassword, libshoutPath);
    }
}
