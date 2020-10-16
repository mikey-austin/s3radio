package net.jackiemclean;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.nio.client.HttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceCastTrackStream implements TrackStream {

    private static final Logger LOG = LoggerFactory.getLogger(IceCastTrackStream.class);

    private final String name;
    private final String contentType;
    private final URI icecastUri;
    private final HttpAsyncClient httpClient;

    private PipedOutputStream streamOut;
    private RateLimitedStreamFactory streamFactory;
    private String description;
    private String genre;
    private Future<HttpResponse> response;

    IceCastTrackStream(String name, String description, String genre, String icecastUri, String contentType,
            HttpAsyncClient httpClient, RateLimitedStreamFactory streamFactory) {
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

        FutureCallback<HttpResponse> cb = new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                LOG.info("icecast stream completed for: {}", name);
            }

            @Override
            public void failed(Exception e) {
                LOG.error("icecast request failure for stream: {}", name, e);
            }

            @Override
            public void cancelled() {
                LOG.info("icecast request cancelled for stream: {}", name);
            }
        };

        this.response = httpClient.execute(httpPut, cb);
    }

    @Override
    public void stop() {
        this.response.cancel(true);
    }
}
