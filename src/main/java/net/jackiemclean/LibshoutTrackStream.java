package net.jackiemclean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gmail.kunicins.olegs.libshout.Libshout;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibshoutTrackStream implements TrackStream {

    private static final Logger LOG = LoggerFactory.getLogger(LibshoutTrackStream.class);

    private final String name;
    private final URI icecastUri;
    private final String password;
    private final String libshoutPath;
    private final AtomicReference<Track> currentTrack;
    private final AtomicInteger currentTrackBytesSoFar;
    private final int streamFormat;

    private Libshout icecast;

    LibshoutTrackStream(String name, String icecastUri, String password, String libshoutPath) {
        this.name = name;
        this.password = password;
        this.icecastUri = UriBuilder.fromUri(icecastUri).path(name).build();
        this.libshoutPath = libshoutPath;
        this.currentTrack = new AtomicReference<>();
        this.currentTrackBytesSoFar = new AtomicInteger();
        this.streamFormat = name.endsWith(".mp3") ? Libshout.FORMAT_MP3 : Libshout.FORMAT_OGG;
    }

    @Override
    @JsonProperty("nowPlaying")
    public Optional<Track> getNowPlaying() {
        return Optional.ofNullable(currentTrack.get());
    }

    @Override
    @JsonIgnore
    public String getTrackStreamUri() {
        return icecastUri.toString();
    }

    @Override
    @JsonProperty("percentPlayed")
    public int getPercentPlayed() {
        Track track = currentTrack.get();
        if (track == null || track.getSizeInBytes() <= 0) {
            return 0;
        }

        double bytesSoFar = currentTrackBytesSoFar.get();
        double proportionPlayed = bytesSoFar / track.getSizeInBytes();
        return (int) (proportionPlayed * 100);
    }

    @Override
    public long play(Track track) throws IOException {
        LOG.info("streaming track: {}", track);
        currentTrack.set(track);
        currentTrackBytesSoFar.set(0);

        String extension = streamFormat == Libshout.FORMAT_MP3 ? ".mp3" : ".ogg";
        File tmpFile = File.createTempFile("stream-buffer", extension);
        try (OutputStream tmpFileOutput = new FileOutputStream(tmpFile);
                InputStream onDiskStream = new FileInputStream(tmpFile)) {
            track.getContent().transferTo(tmpFileOutput);
            tmpFileOutput.flush();

            byte[] buf = new byte[1024];
            int nRead = onDiskStream.read(buf);
            currentTrackBytesSoFar.getAndAdd(nRead);
            while (nRead > 0) {
                try {
                    icecast.send(buf, nRead);
                } catch (SocketException e) {
                    LOG.error("caught exception sending to libshout", e);
                }
                nRead = onDiskStream.read(buf);
                currentTrackBytesSoFar.getAndAdd(nRead);

                if (Thread.interrupted()) {
                    break;
                }
            }
        } finally {
            tmpFile.delete();
        }

        return currentTrackBytesSoFar.get();
    }

    @Override
    public void start() throws IOException {
        this.icecast = new Libshout(Paths.get(libshoutPath));
        icecast.setHost(icecastUri.getHost());
        icecast.setPort(icecastUri.getPort());
        icecast.setProtocol(Libshout.PROTOCOL_HTTP);
        icecast.setPassword(password);
        icecast.setMount(name);
        icecast.setFormat(streamFormat);
        icecast.setName(Files.getNameWithoutExtension(name));
        icecast.open();
    }

    @Override
    public void stop() {
        icecast.close();
    }
}
