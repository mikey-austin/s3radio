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

import com.gmail.kunicins.olegs.libshout.Libshout;

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

    private Libshout icecast;

    LibshoutTrackStream(String name, String icecastUri, String password, String libshoutPath) {
        this.name = name;
        this.password = password;
        this.icecastUri = UriBuilder.fromUri(icecastUri).path(name).build();
        this.libshoutPath = libshoutPath;
        this.currentTrack = new AtomicReference<>();
        this.currentTrackBytesSoFar = new AtomicInteger();
    }

    @Override
    public Optional<Track> getNowPlaying() {
        return Optional.ofNullable(currentTrack.get());
    }

    @Override
    public int getPercentPlayed() {
        Track track = currentTrack.get();
        int bytesSoFar = currentTrackBytesSoFar.get();
        if (track == null || track.getSizeInBytes() <= 0) {
            return 0;
        }
        return Math.round((bytesSoFar / track.getSizeInBytes()) * 100);
    }

    @Override
    public long play(Track track) throws IOException {
        LOG.info("streaming track: {}", track);
        currentTrack.set(track);
        currentTrackBytesSoFar.set(0);

        File tmpFile = File.createTempFile("stream-buffer", ".ogg");
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
        icecast.setFormat(Libshout.FORMAT_OGG);
        icecast.open();
    }

    @Override
    public void stop() {
        icecast.close();
    }
}
