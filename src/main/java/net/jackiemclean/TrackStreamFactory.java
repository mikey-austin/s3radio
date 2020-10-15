package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.nio.client.HttpAsyncClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TrackStreamFactory {

    private final HttpAsyncClient httpClient;
    private final RateLimitedOutputStreamFactory streamFactory;
    private final String icecastBaseUri;

    @Inject
    public TrackStreamFactory(HttpAsyncClient httpClient, RateLimitedOutputStreamFactory streamFactory,
            @ConfigProperty(name = "icecast.baseUri") String icecastBaseUri) {
        this.httpClient = httpClient;
        this.streamFactory = streamFactory;
        this.icecastBaseUri = icecastBaseUri;
    }

    public TrackStream createStream(Station station) {
        // We only support ogg streams.
        String streamName = station.getName() + ".ogg";

        // TODO: find a better way to obtain the stream metadata.
        return new IceCastTrackStream(streamName, "private radio", "jazz", icecastBaseUri, "audio/ogg", httpClient,
                streamFactory.create());
    }
}
