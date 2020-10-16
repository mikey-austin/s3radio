package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.nio.client.HttpAsyncClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TrackStreamFactory {

    private final HttpAsyncClient httpClient;
    private final RateLimitedStreamFactory streamFactory;
    private final String icecastBaseUri;
    private final String sourcePassword;
    private final String libshoutPath;

    @Inject
    public TrackStreamFactory(HttpAsyncClient httpClient, RateLimitedStreamFactory streamFactory,
            @ConfigProperty(name = "icecast.baseUri") String icecastBaseUri,
            @ConfigProperty(name = "icecast.password") String sourcePassword,
            @ConfigProperty(name = "icecast.libshoutPath") String libshoutPath) {
        this.httpClient = httpClient;
        this.streamFactory = streamFactory;
        this.icecastBaseUri = icecastBaseUri;
        this.sourcePassword = sourcePassword;
        this.libshoutPath = libshoutPath;
    }

    public TrackStream createStream(Station station) {
        // We only support ogg streams.
        String streamName = station.getName() + ".ogg";
        return new LibshoutTrackStream(streamName, icecastBaseUri, sourcePassword, libshoutPath);
    }
}
