package net.jackiemclean;

import java.io.IOException;
import java.io.PipedInputStream;
import java.net.URI;
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

    private RateLimitedOutputStream streamOut;
    private String description;
    private String genre;
    private Future<HttpResponse> response;

    IceCastTrackStream(String name, String description, String genre, String icecastUri, String contentType,
                       HttpAsyncClient httpClient, RateLimitedOutputStream streamOut) {
        this.name = name;
        this.description = description;
        this.genre = genre;
        this.contentType = contentType;
        this.icecastUri = UriBuilder.fromUri(icecastUri).path(name).build();
        this.httpClient = httpClient;
        this.streamOut = streamOut;
    }

    @Override
    public long play(Track track) throws IOException {
        return track.getContent().transferTo(this.streamOut);
    }

    @Override
    public void start() throws IOException {
        PipedInputStream streamIn = new PipedInputStream(this.streamOut.getDelegate());
        HttpEntity streamEntity = new InputStreamEntity(streamIn);

        HttpPut httpPut = new HttpPut(icecastUri);
        httpPut.setEntity(streamEntity);
        httpPut.expectContinue();
        httpPut.setHeader("Content-type", contentType);
        httpPut.setHeader("Ice-Name", name);
        httpPut.setHeader("Ice-Description", description);
        httpPut.setHeader("Ice-Genre", genre);

        this.response = this.httpClient.execute(httpPut, new FutureCallback<HttpResponse>() {
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
        });
    }

    @Override
    public void stop() {
        this.response.cancel(true);
    }
}
