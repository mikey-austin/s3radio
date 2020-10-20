package net.jackiemclean;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceCastTrackStream implements TrackStream {

    private static final Logger LOG = LoggerFactory.getLogger(IceCastTrackStream.class);

    private final String name;
    private final String contentType;
    private final URI icecastUri;
    private final HttpClient httpClient;

    private PipedOutputStream streamOut;
    private RateLimitedStreamFactory streamFactory;
    private String description;
    private String genre;
    private Future<HttpResponse> response;

    IceCastTrackStream(String name, String description, String genre, String icecastUri, String contentType,
            HttpClient httpClient, RateLimitedStreamFactory streamFactory) {
        this.name = name;
        this.description = description;
        this.genre = genre;
        this.contentType = contentType;
        this.icecastUri = UriBuilder.fromUri(icecastUri).path(name).build();
        this.httpClient = httpClient;
        this.streamFactory = streamFactory;
        this.streamOut = new PipedOutputStream();
    }

    @Override
    public Optional<Track> getNowPlaying() {
        return Optional.empty();
    }

    @Override
    public int getPercentPlayed() {
        return 0;
    }

    @Override
    public long play(Track track) throws IOException {
        LOG.info("about to stream track: {}", track);
        return streamFactory.limitInputStream(track.getContent()).transferTo(this.streamOut);
    }

    @Override
    public void start() throws IOException {
        PipedInputStream streamIn = new PipedInputStream();
        streamIn.connect(streamOut);
        HttpEntity streamEntity = new InputStreamEntity(streamIn);

        HttpPut httpPut = new HttpPut(icecastUri);
        httpPut.setEntity(streamEntity);
        httpPut.expectContinue();
        httpPut.setHeader("Authorization", "Basic c291cmNlOmhhY2ttZQ=="); // TODO
        httpPut.setHeader("Content-type", contentType);
        httpPut.setHeader("Ice-Name", name);
        httpPut.setHeader("Ice-Description", description);
        httpPut.setHeader("Ice-Genre", genre);
        httpPut.setHeader("Ice-Audio-Info", "samplerate=44100;quality=10%2e0;channels=2"); // TODO

        this.response = CompletableFuture.supplyAsync(() -> {
            try {
                return httpClient.execute(httpPut);
            } catch (Exception e) {
                LOG.error("couldn't reach icecast", e);
            }
            return null;
        });
    }

    @Override
    public void stop() {
        this.response.cancel(true);
    }

    @Override
    public String getTrackStreamUri() {
        return icecastUri.toString();
    }
}
