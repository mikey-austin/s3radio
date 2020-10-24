package net.jackiemclean;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TrackStreamFactory {

    private final String icecastBaseUri;
    private final String publicIcecastBaseUri;
    private final String sourcePassword;
    private final String libshoutPath;

    @Inject
    public TrackStreamFactory(
            @ConfigProperty(name = "icecast.baseUri") String icecastBaseUri,
            @ConfigProperty(name = "icecast.password") String sourcePassword,
            @ConfigProperty(name = "icecast.libshoutPath", defaultValue = "/tmp/libshout-java.so")
                    String libshoutPath,
            @ConfigProperty(name = "icecast.publicBaseUri") String publicIcecastBaseUri) {
        this.icecastBaseUri = icecastBaseUri;
        this.sourcePassword = sourcePassword;
        this.libshoutPath = libshoutPath;
        this.publicIcecastBaseUri = publicIcecastBaseUri;
    }

    public TrackStream createStream(Station station) {
        return new LibshoutTrackStream(
                station.getName(),
                icecastBaseUri,
                sourcePassword,
                libshoutPath,
                publicIcecastBaseUri);
    }
}
